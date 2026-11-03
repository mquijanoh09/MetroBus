"""Reference prototype (Python) of the routing algorithm described in docs/algorithm.md.

It is the behavioral reference for the Java implementation: the Java app must reproduce
data/test_cases.csv (same cost and vehicles, duration within +/-1 minute).

Usage (from the kiro/ folder):
    python3 -I reference/prototype.py data "<origin address>" "<destination address>"
    python3 -I reference/prototype.py data --tests        # 300 random trips + edge cases
    python3 -I reference/prototype.py data --make-cases   # regenerates data/test_cases.csv

User-facing text is in Spanish on purpose: the app is for Medellín riders.
Field names inside data/raw/*.json are the official Spanish names from the Alcaldía de Medellín.
"""
import csv, json, glob, collections, heapq, itertools, math, re, sys, random, time

# ---------------- Parameters (docs/graph-structure.md, section 4) ----------------
BUS_SPEED_KMH = {'Directa Centro': 15, 'Circular': 15, 'Integrada': 18, 'Directa No Centro': 18}
STOP_DWELL_SEC = 20
WAIT_SEC = {'bus': 300, 'metro': 240, 'brt': 300, 'tram': 300, 'cable_car': 120}
BOARD_PENALTY_SEC = 120            # perceived cost only
WALK_FACTOR, WALK_PENALTY_SEC = 1.5, 30   # perceived cost only
WALK_SPEED_MS = 4.5 / 3.6
STREET_FACTOR = 1.3                # city walking distance vs straight line
ACCESS_RADIUS_M = 500
SAME_PLACE_M = 50
FARE_WINDOW_SEC = 90 * 60
FARE_TRANSIT, FARE_BUS, FARE_INTEGRATED = 3820, 3800, 4715   # COP, frequent rider with Cívica
TRANSIT_SURCHARGE, SURCHARGE_AT = 750, (5, 8, 9)            # integration count that adds a surcharge
LINE_LABEL = {'metro': 'Metro Línea {}', 'brt': 'Metroplús Línea {}', 'tram': 'Tranvía de Ayacucho', 'cable_car': 'Metrocable Línea {}'}

def haversine_m(la1, lo1, la2, lo2):
    R = 6371000; p1, p2 = math.radians(la1), math.radians(la2)
    a = math.sin((p2 - p1) / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(math.radians(lo2 - lo1) / 2) ** 2
    return 2 * R * math.asin(math.sqrt(a))

def walk_sec(m): return m * STREET_FACTOR / WALK_SPEED_MS

# ---------------- Address -> (calle, carrera) ----------------
STREET = {'CL': 'CL', 'CALLE': 'CL', 'DG': 'CL', 'DIAGONAL': 'CL',
          'CR': 'CR', 'KR': 'CR', 'CRA': 'CR', 'CARRERA': 'CR', 'TV': 'CR', 'TRANSVERSAL': 'CR'}
def _number(s, south=False):
    m = re.match(r'(\d+)\s*([A-Z]{0,3})', s.strip())
    if not m: return None
    n = int(m.group(1)) + (0.3 * (ord(m.group(2)[0]) - 64) / 4 if m.group(2) and m.group(2)[0] in 'ABCDEFG' else 0)
    return -n if south else n

def parse_address(text):
    """Returns (calle, carrera) as numbers, or None if the address cannot be interpreted."""
    if not text: return None
    d = text.upper().split(',')[0].replace('#', ' # ').replace('N°', ' # ').replace('NO.', ' # ')
    d = re.sub(r'\s+', ' ', d).strip()
    m = re.match(r'(CL|CR|KR|CRA|DG|TV|CALLE|CARRERA|DIAGONAL|TRANSVERSAL)\.?\s+(\d+\s?[A-Z]{0,3}(?:\s+BIS)?)\s*(SUR|ESTE)?\s*(?:X|CON|#)?\s*'
                 r'(?:(CL|CR|KR|CRA|DG|TV|CALLE|CARRERA)\.?\s+)?(\d+\s?[A-Z]{0,3})\s*(SUR|ESTE)?(?:\s|-|$)', d)
    if not m: return None
    kind = STREET[m.group(1)]
    a = _number(m.group(2), m.group(3) == 'SUR'); b = _number(m.group(5), m.group(6) == 'SUR')
    if a is None or b is None: return None
    return (a, b) if kind == 'CL' else (b, a)

# ---------------- Network ----------------
class Network:
    def __init__(self, data_dir):
        rows = []
        for f in sorted(glob.glob(data_dir + '/raw/stops_*.json')):
            rows += [x['attributes'] for x in json.load(open(f))['features']]
        self.place = {}   # key -> (display name, lat, lon)
        self.route = {}   # route_id -> (code, name, type)
        for p in rows:
            self.place['STOP:' + str(p['id_paradero'])] = ((p['direccion'] or '').split(',')[0].strip(), p['latitud'], p['longitud'])
            self.route[p['id_ruta']] = (p['codigo_ruta'], p['nombre_ruta'], p['tipo_ruta'])
        self.lines = collections.defaultdict(list)
        for r in csv.DictReader(open(data_dir + '/transit_lines.csv', encoding='utf-8')):
            self.lines[r['line']].append(r)
            self.place.setdefault('STATION:' + r['station'], ('Estación ' + r['station'], float(r['lat']), float(r['lon'])))

        # node = (place, mode); mode = 'WALK' | (route_id, direction) | ('L' + line, '+'/'-')
        self.G = collections.defaultdict(list)   # u -> [(v, perceived_cost, real_sec, kind)]
        def edge(u, v, real, perceived, kind):
            self.G[u].append((v, perceived, real, kind)); self.G.setdefault(v, [])
        for r in csv.DictReader(open(data_dir + '/bus_segments.csv')):
            rid = int(r['route_id']); t = float(r['used_m']) / (BUS_SPEED_KMH[self.route[rid][2]] / 3.6) + STOP_DWELL_SEC
            edge(('STOP:' + r['from_stop_id'], rid, r['direction']), ('STOP:' + r['to_stop_id'], rid, r['direction']), t, t, 'ride')
        # link the end of the outbound trip to the start of the inbound trip (same bus keeps going)
        seq = collections.defaultdict(list)
        for p in rows: seq[(p['id_ruta'], p['recorrido'])].append(p)
        for rid in {k[0] for k in seq}:
            a, b = seq.get((rid, 'Origen-Destino')), seq.get((rid, 'Destino-Origen'))
            if a and b:
                x = max(a, key=lambda q: q['nro_parada']); y = min(b, key=lambda q: q['nro_parada'])
                d = math.hypot(x['x_origen_nacional'] - y['x_origen_nacional'], x['y_origen_nacional'] - y['y_origen_nacional'])
                t = d * STREET_FACTOR / (BUS_SPEED_KMH[self.route[rid][2]] / 3.6)
                edge(('STOP:' + str(x['id_paradero']), rid, 'outbound'), ('STOP:' + str(y['id_paradero']), rid, 'inbound'), t, t, 'ride')
        for line, stations in self.lines.items():
            for a, b in zip(stations, stations[1:]):
                t = int(a['sec_to_next'])
                edge(('STATION:' + a['station'], 'L' + line, '+'), ('STATION:' + b['station'], 'L' + line, '+'), t, t, 'ride')
                edge(('STATION:' + b['station'], 'L' + line, '-'), ('STATION:' + a['station'], 'L' + line, '-'), t, t, 'ride')
        for s in [s for s in list(self.G) if s[1] != 'WALK']:
            wait = WAIT_SEC['bus'] if isinstance(s[1], int) else WAIT_SEC[self.lines[s[1][1:]][0]['mode']]
            edge((s[0], 'WALK'), s, wait, wait + BOARD_PENALTY_SEC, 'board')
            edge(s, (s[0], 'WALK'), 0, 0, 'alight')
        for r in csv.DictReader(open(data_dir + '/walk_links.csv', encoding='utf-8')):
            t = int(r['walk_sec'])
            for a, b in ((r['place_a'], r['place_b']), (r['place_b'], r['place_a'])):
                edge((a, 'WALK'), (b, 'WALK'), t, t * WALK_FACTOR + WALK_PENALTY_SEC, 'walk')
        # reference points for locating addresses
        self.address_ref = [(c, pl) for k, pl in self.place.items() if k.startswith('STOP:') for c in [parse_address(pl[0])] if c]

    def locate(self, text):
        """Address -> (lat, lon) by interpolating the 4 stops with the closest (calle, carrera)."""
        c = parse_address(text)
        if not c: return None
        near = sorted(self.address_ref, key=lambda t: (t[0][0] - c[0]) ** 2 + (t[0][1] - c[1]) ** 2)[:4]
        if math.hypot(near[0][0][0] - c[0], near[0][0][1] - c[1]) > 6: return None   # ~6 blocks from any known stop
        w = [1 / (1e-3 + math.hypot(t[0][0] - c[0], t[0][1] - c[1])) for t in near]
        return (sum(a * t[1][1] for a, t in zip(w, near)) / sum(w), sum(a * t[1][2] for a, t in zip(w, near)) / sum(w))

    def nearby(self, la, lo, radius=ACCESS_RADIUS_M):
        return [(k, haversine_m(la, lo, v[1], v[2])) for k, v in self.place.items()
                if abs(v[1] - la) < 0.006 and haversine_m(la, lo, v[1], v[2]) <= radius]

    # ---------------- Multi-source / multi-target Dijkstra ----------------
    def walk_only(self, m):
        return {'steps': [{'kind': 'walk', 'm': m, 'sec': walk_sec(m), 'to': 'tu destino'}], 'sec': walk_sec(m), 'cost': 0, 'walk_only': True}

    def find_route(self, origin, dest):
        direct = haversine_m(*origin, *dest)
        if direct < SAME_PLACE_M: return {'error': 'already_there', 'direct_m': direct}
        sources = self.nearby(*origin); targets = dict(self.nearby(*dest))
        if not sources or not targets: return {'error': 'no_stops_nearby', 'direct_m': direct}
        tie = itertools.count(); dist = {}; real = {}; prev = {}; pq = []
        for k, m in sources:
            s = (k, 'WALK'); t = walk_sec(m)
            if t * WALK_FACTOR + WALK_PENALTY_SEC < dist.get(s, 1e18):
                dist[s] = t * WALK_FACTOR + WALK_PENALTY_SEC; real[s] = t; prev[s] = ('ORIGIN', 'walk', m)
                heapq.heappush(pq, (dist[s], next(tie), s))
        best, best_state = walk_sec(direct) * WALK_FACTOR + WALK_PENALTY_SEC, None   # walking all the way is always an option
        while pq:
            du, _, u = heapq.heappop(pq)
            if du >= best: break                       # nothing left in the queue can improve the best total
            if du > dist[u]: continue                  # stale queue entry
            if u[1] == 'WALK' and u[0] in targets:     # only finish on foot
                total = du + walk_sec(targets[u[0]]) * WALK_FACTOR + WALK_PENALTY_SEC
                if total < best: best, best_state = total, u
            for v, perceived, rs, kind in self.G[u]:
                nd = du + perceived
                if nd < dist.get(v, 1e18):
                    dist[v] = nd; real[v] = real[u] + rs; prev[v] = (u, kind, rs); heapq.heappush(pq, (nd, next(tie), v))
        if best_state is None: return self.walk_only(direct)
        path = []; u = best_state
        while u != 'ORIGIN':
            p = prev[u]; path.append((p[0], u, p[1], p[2])); u = p[0]
        path.reverse()
        r = self.to_steps(path, targets[best_state[0]])
        if walk_sec(direct) <= r['sec']: return self.walk_only(direct)   # rule: if walking is as fast in REAL time, walk
        return r

    # ---------------- Path -> readable steps, real duration and cost ----------------
    def name(self, k): return self.place[k][0]
    def vehicle(self, s):
        """Returns (mode, label shown to the user, route type, short id used in test_cases.csv)."""
        if isinstance(s[1], int):
            code, name, rtype = self.route[s[1]]; return ('bus', f'bus {code} ({name})', rtype, f'bus:{code}')
        line = s[1][1:]; mode = self.lines[line][0]['mode']
        return (mode, LINE_LABEL[mode].format(line), mode, f'{mode}:{line}')

    def to_steps(self, path, final_walk_m):
        steps = []; t = 0
        for u, v, kind, x in path:
            if kind == 'walk':
                m = x if u == 'ORIGIN' else x * WALK_SPEED_MS / STREET_FACTOR   # x = meters from origin, else seconds
                sec = walk_sec(x) if u == 'ORIGIN' else x
                if steps and steps[-1]['kind'] == 'walk':
                    steps[-1]['m'] += m; steps[-1]['sec'] += sec; steps[-1]['to'] = self.name(v[0])
                else:
                    steps.append({'kind': 'walk', 'm': m, 'sec': sec, 'to': self.name(v[0]), 't0': t})
                t += sec
            elif kind == 'board':
                mode, label, rtype, short = self.vehicle(v)
                steps.append({'kind': 'ride', 'mode': mode, 'route_type': rtype, 'label': label, 'short': short,
                              'from': self.name(v[0]), 'wait': x, 'sec': x, 'stops': 0, 't0': t})
                t += x
            elif kind == 'ride':
                steps[-1]['sec'] += x; steps[-1]['stops'] += 1; steps[-1]['to'] = self.name(v[0]); t += x
        if final_walk_m > 0:
            sec = walk_sec(final_walk_m)
            if steps[-1]['kind'] == 'walk':
                steps[-1]['m'] += final_walk_m; steps[-1]['sec'] += sec; steps[-1]['to'] = 'tu destino'
            else:
                steps.append({'kind': 'walk', 'm': final_walk_m, 'sec': sec, 'to': 'tu destino', 't0': t})
            t += sec
        return {'steps': steps, 'sec': t, 'cost': self.fare(steps)}

    def fare(self, steps):
        """docs/fares.md: frequent rider with Cívica, 90-minute integration window."""
        cost = 0; transit_start = None; integrations = 0; last = None
        for p in steps:
            if p['kind'] != 'ride': continue
            if p['mode'] != 'bus':
                if transit_start is not None and p['t0'] - transit_start <= FARE_WINDOW_SEC:
                    integrations += 1                                   # transfer inside the mass-transit system
                    if integrations in SURCHARGE_AT: cost += TRANSIT_SURCHARGE
                elif last and last['mode'] == 'bus' and last['route_type'] == 'Integrada' \
                        and p['t0'] - last['t0'] <= FARE_WINDOW_SEC and not last.get('integrated'):
                    cost += FARE_INTEGRATED - FARE_BUS                  # integrated bus -> transit: pair costs 4.715
                    transit_start = last['t0']; integrations = 1; last['integrated'] = True
                else:
                    cost += FARE_TRANSIT; transit_start = p['t0']; integrations = 1
            else:
                if p['route_type'] == 'Integrada' and transit_start is not None and p['t0'] - transit_start <= FARE_WINDOW_SEC \
                        and last and last['mode'] != 'bus' and not last.get('integrated'):
                    cost += FARE_INTEGRATED - FARE_TRANSIT; p['integrated'] = True   # transit -> integrated bus
                else:
                    cost += FARE_BUS
            last = p
        return cost

def print_route(r):
    if 'error' in r:
        print({'already_there': '  Ya estás en tu destino.', 'no_stops_nearby': '  No hay paraderos ni estaciones cerca de esa dirección.'}[r['error']]); return
    for i, p in enumerate(r['steps'], 1):
        if p['kind'] == 'walk': print(f"  {i}. Camina {p['m']:.0f} m (~{p['sec']/60:.0f} min) hasta {p['to']}")
        else: print(f"  {i}. En {p['from']} toma {p['label']}; bájate en {p['to']} ({p['stops']} paradas, ~{p['sec']/60:.0f} min con espera)")
    print(f"  Total: {r['sec']/60:.0f} min · ${r['cost']:,}".replace(',', '.'))

CASES = [('CL 50 53-48', 'CR 70 x CL 48'), ('CL 10 43A-20', 'CL 77 65-30'), ('CR 80 32-15', 'CL 51 46-10'),
         ('CL 30 65-10', 'CL 30 66-20'), ('CL 44 80-10', 'CL 33 65-20')]

if __name__ == '__main__':
    t0 = time.time(); net = Network(sys.argv[1]); print(f'graph loaded in {time.time()-t0:.1f} s: {len(net.G)} nodes')
    mode = sys.argv[2]
    if mode == '--make-cases':
        random.seed(3)
        with_addr = [v[0] for k, v in net.place.items() if k.startswith('STOP:') and parse_address(v[0])]
        pairs = CASES + [tuple(random.sample(with_addr, 2)) for _ in range(10)]
        with open(sys.argv[1] + '/test_cases.csv', 'w', newline='', encoding='utf-8') as f:
            w = csv.writer(f); w.writerow(['origin', 'destination', 'minutes', 'cost_cop', 'transfers', 'vehicles'])
            for o, d in pairs:
                r = net.find_route(net.locate(o), net.locate(d)); v = [p['short'] for p in r['steps'] if p['kind'] == 'ride']
                w.writerow([o, d, round(r['sec'] / 60), r['cost'], max(len(v) - 1, 0), ' > '.join(v) or 'walk'])
        print('test_cases.csv written'); sys.exit()
    if mode == '--tests':
        random.seed(11)
        with_addr = [k for k, v in net.place.items() if k.startswith('STOP:') and parse_address(v[0])]
        ms, transfers, walked, slower, reboard, failed, walk_only = [], [], [], 0, 0, 0, 0
        for _ in range(300):
            a, b = random.sample(with_addr, 2)
            o, d = net.locate(net.place[a][0]), net.locate(net.place[b][0])
            if not o or not d: failed += 1; continue
            t = time.time(); r = net.find_route(o, d); ms.append((time.time() - t) * 1000)
            if 'error' in r: failed += 1; continue
            walk_only += bool(r.get('walk_only'))
            rides = [p for p in r['steps'] if p['kind'] == 'ride']
            transfers.append(max(len(rides) - 1, 0)); walked.append(sum(p['m'] for p in r['steps'] if p['kind'] == 'walk'))
            slower += r['sec'] > walk_sec(haversine_m(*o, *d)) + 1
            reboard += sum(1 for x, y in zip(rides, rides[1:]) if x['label'] == y['label'])
        q = lambda L, f: sorted(L)[int(f * (len(L) - 1))]
        print(f'trips 300, failed {failed}, walk only {walk_only}')
        print(f'query time: median {q(ms,.5):.0f} ms, p95 {q(ms,.95):.0f} ms')
        print(f'transfers: median {q(transfers,.5)}, p95 {q(transfers,.95)}, max {max(transfers)}')
        print(f'walking per trip: median {q(walked,.5):.0f} m, p95 {q(walked,.95):.0f} m')
        print(f'slower than walking: {slower}; re-boarding the same vehicle: {reboard}')
        for txt in ['hola mundo', 'CL 900 x CR 900', 'VDA EL YOLOMBO', 'cl 50 # 53-48', 'Calle 10 # 43A - 20', 'CL 12 Sur x CR 50']:
            print(f'  locate({txt!r}) -> {net.locate(txt)}')
        o = net.locate('CL 50 53-48'); print('  same origin and destination ->', net.find_route(o, o))
        sys.exit()
    o, d = net.locate(sys.argv[2]), net.locate(sys.argv[3])
    if not o or not d: print('  No reconozco la dirección:', sys.argv[2] if not o else sys.argv[3]); sys.exit(1)
    print_route(net.find_route(o, d))
