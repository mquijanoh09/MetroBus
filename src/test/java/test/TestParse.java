package test;
import co.medellin.transit.geocoding.*;
import java.util.*;
public class TestParse {
public static void main(String[] args) {
AddressParser p = new AddressParser();
String addr = "CL 50 53-48";
Optional<ParsedAddress> pa = p.parse(addr);
System.out.println(addr + " parse: " + pa.isPresent());
if (pa.isPresent()) {
ParsedAddress a = pa.get();
System.out.println("  Type: " + a.getStreetType());
System.out.println("  Primary: " + a.getPrimaryNumber());
System.out.println("  Secondary: " + a.getSecondaryNumber());
}
}
}