package test;
import co.medellin.transit.geocoding.*;
import java.util.*;
public class TestNormalize {
public static void main(String[] args) throws Exception {
AddressParser p = new AddressParser();
java.lang.reflect.Method m = AddressParser.class.getDeclaredMethod("normalize", String.class);
m.setAccessible(true);
String addr = "CL 50 53-48";
String normalized = (String) m.invoke(p, addr);
System.out.println("Input: " + addr);
System.out.println("Normalized: " + normalized);
Optional<ParsedAddress> pa = p.parse(addr);
System.out.println("Parses: " + pa.isPresent());
}
}