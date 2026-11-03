# Fares 2026 and Integration Rules (for `FareCalculator`)

**In force since:** January 10, 2026 · **Checked:** October 29, 2026
**Default profile:** frequent rider paying with a personal Cívica card. All amounts are in Colombian pesos (COP).

## 1. Price per boarding

| What the rider boards | Frequent (Cívica) | Bearer / occasional | Source |
|---|---|---|---|
| Metro, tram, cable car or Metroplús | **3,820** | 4,400 | Acuerdo Metropolitano 02/2026, art. 3 |
| Urban bus or buseta in Medellín | **3,800** | 3,800 | Alcaldía de Medellín |
| Urban microbus | 3,900 | 3,900 | Alcaldía de Medellín (the data does not say which routes use microbuses, so the app uses 3,800) |
| **Integrated** bus route + Metro, as a pair | **4,715 total** | no integrated fare | press, art. 4; confirmed by the product owner on Oct 29 |

Other mass-transit profiles: student 1,600, senior 3,330, disability 2,720. These discounts **do not apply** to integrations with buses (art. 4, par. 5).

## 2. Integration rules

1. **Inside the mass-transit system** (Metro ↔ cable car ↔ tram ↔ Metroplús), within 90 minutes with Cívica, the price depends on the **number of integrations** in the trip, not on which modes are used:

   | Integrations | Frequent | Bearer |
   |---|---|---|
   | up to 4 (the common case) | **3,820** | 4,400 |
   | 5 to 7 | 4,570 | 5,150 |
   | 8 | 5,320 | 5,900 |
   | 9 to 10 | 6,070 | 6,650 |

   So Metro + Metroplús, or Metro + cable car, adds **no surcharge** up to 4 integrations. A +750 surcharge is added when the count reaches 5, 8 and 9.
2. **Integrated bus + Metro:** Cívica only, routes with `tipo_ruta = Integrada` only, and **the Metro must be part of the trip**. Everything must happen within 90 minutes of the first payment.
   - Single integration (integrated bus → Metro, or Metro → integrated bus): the pair costs 4,715.
   - Double integration (integrated bus → Metro → integrated bus, the two buses in different *cuencas*): the official rule is the sum of each mode's value from the art. 4 table. **Pending:** that table was not legible. The reference prototype integrates only one bus and charges the other one 3,800.
3. **Integrated bus without the Metro:** full bus fare.
4. **Bus → bus, or non-integrated bus ↔ Metro:** no integration; each boarding pays full fare. This covers the `Directa Centro`, `Directa No Centro` and `Circular` routes.

## 3. Algorithm (implemented in `reference/prototype.py`, `Network.fare`)

Iterate over the ride steps in order. Each step has `t0`, the seconds since departure.

```
cost = 0; transitStart = null; integrations = 0; last = null
for each ride step p:
  if p is mass transit:
     if transitStart != null and p.t0 − transitStart ≤ 90 min:      // transfer inside mass transit
         integrations += 1; if integrations in {5, 8, 9}: cost += 750
     else if last is an Integrada bus, ≤ 90 min, not yet integrated:  // integrated bus → transit
         cost += 4,715 − 3,800; transitStart = last.t0; integrations = 1; mark last integrated
     else:
         cost += 3,820; transitStart = p.t0; integrations = 1
  if p is a bus:
     if p is Integrada, last was transit and not integrated, and ≤ 90 min since transitStart:
         cost += 4,715 − 3,820; mark p integrated                  // transit → integrated bus
     else:
         cost += 3,800
  last = p
```

The route is chosen by **time**, not by price.

## Sources

- Acuerdo Metropolitano 02 de 2026: https://www.iush.edu.co/uploads/aterrizaje/2026/Anexo-3-Acuerdo-Metropolitano-2-de-2026.pdf
- El Espectador (integration tiers): https://www.elespectador.com/colombia/medellin/asi-quedo-el-pasaje-del-metro-tranvia-y-rutas-integradas-en-medellin/
- Cívica, integrated routes: https://civica.metrodemedellin.gov.co/como-usarla/rutas-integradas
- Alcaldía de Medellín, 2026 urban bus fare: https://www.medellin.gov.co/es/sala-de-prensa/noticias/desde-el-sabado-10-de-enero-habra-ajuste-en-la-tarifa-del-transporte-publico-colectivo-de-medellin/
- Telemedellín: https://telemedellin.tv/tarifas-transporte-publico-medellin-2026/
