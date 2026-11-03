# Task 8.1: AddressParser Implementation Summary

## Overview
Implemented the AddressParser component for parsing Medellín street addresses according to the exact algorithm specification.

## Files Created

### Production Code
1. **StreetType.java** - Enum for street types (CALLE, CARRERA, DIAGONAL, CIRCULAR, TRANSVERSAL)
   - Handles synonyms: CL, CR, KR, CRA, DG, TV
   - Case-insensitive parsing

2. **Cardinal.java** - Enum for cardinal directions (NORTE, SUR, ESTE, OESTE)
   - SUR is particularly important as it makes address numbers negative

3. **ParsedAddress.java** - Data class representing a parsed address
   - Fields: streetType, primaryNumber, secondaryNumber, buildingNumber (optional), cardinal (optional)
   - Supports fractional numbers for letter suffixes (43A → 43.075)
   - Supports negative numbers for SUR suffix (12 Sur → -12)

4. **AddressParser.java** - Main parser implementing the algorithm
   - **Step 1: Normalize** - Upper case, drop after comma, normalize separators (#, No., N°), collapse spaces
   - **Step 2: Parse** - Regex pattern matching: STREET NUMBER[LETTER] [SUR] [SEPARATOR] [STREET] NUMBER[LETTER] [SUR] [- BUILDING]
   - **Step 3: Letter handling** - Convert A=0.075, B=0.15, C=0.225, etc.
   - **Step 4: SUR handling** - Make numbers negative when SUR present
   - **Validation** - Reject addresses with numbers > 300 (out of range)

5. **package-info.java** - Package documentation

### Test Code
1. **AddressParserTest.java** - Comprehensive unit tests (40+ test cases)
   - Valid address formats
   - Edge cases (case sensitivity, whitespace, synonyms)
   - Invalid formats (rural addresses, out of range, malformed)
   - Letter conversion tests
   - SUR handling tests

2. **StreetTypeTest.java** - Unit tests for StreetType enum
   - All synonyms and abbreviations
   - Case sensitivity
   - Invalid input handling

3. **CardinalTest.java** - Unit tests for Cardinal enum
   - All cardinal directions
   - Case sensitivity
   - Invalid input handling

4. **ParsedAddressTest.java** - Unit tests for ParsedAddress class
   - Constructor and getters
   - Optional fields
   - Fractional numbers
   - Negative numbers (SUR)
   - toString() method

## Algorithm Implementation

### Normalization Rules (Exact per spec)
1. Convert to upper case
2. Drop everything after first comma
3. Normalize separators: #, No., N° all treated as #
4. Collapse multiple spaces to single space
5. Trim leading/trailing whitespace

### Pattern Matching
Regex pattern matches:
```
STREETTYPE NUMBER[LETTER] [CARDINAL] SEPARATOR [STREETTYPE] NUMBER[LETTER] [CARDINAL] [- BUILDING]
```

Where:
- STREETTYPE: CL, CALLE, CR, KR, CRA, CARRERA, DG, DIAGONAL, TV, TRANSVERSAL, CIRCULAR
- NUMBER: digits
- LETTER: optional A-Z suffix
- CARDINAL: optional SUR, NORTE, ESTE, OESTE
- SEPARATOR: #, X, CON
- BUILDING: optional building number after dash

### Letter Conversion
Letter suffixes are converted to fractional values:
- A = base + 0.075
- B = base + 0.150
- C = base + 0.225
- ... (each letter adds 0.075)

Example: 43A → 43.075, 43B → 43.15

### SUR Handling
SUR cardinal direction makes the associated number negative:
- "CL 12 Sur # 50" → primary = -12
- "CR 70 x CL 48 SUR" → secondary = -48

### Validation
Addresses are rejected if:
- Does not match the pattern format
- Street type is not recognized
- Numbers are > 300 (out of Medellín range)
- Rural address format (e.g., "VDA EL YOLOMBO")

## Test Coverage

### Valid Test Cases (per spec)
✓ "cl 50 # 53-48" → CALLE 50, CARRERA 53, building 48
✓ "Calle 10 # 43A - 20" → CALLE 10, CARRERA 43.075, building 20
✓ "CL 12 Sur x CR 50" → CALLE -12, CARRERA 50
✓ "CR 70 x CL 48 SUR" → CARRERA 70, CALLE -48

### Invalid Test Cases (per spec)
✗ "hola mundo" - not an address
✗ "VDA EL YOLOMBO" - rural address
✗ "CL 900 x CR 900" - out of range

### Additional Test Cases
- All street type synonyms (CL, CR, KR, CRA, DG, TV, etc.)
- All cardinal directions (NORTE, SUR, ESTE, OESTE)
- Multiple letter suffixes (A, B, C, ..., Z)
- Different separators (#, No., N°, X, CON)
- With and without building numbers
- Case insensitivity
- Extra whitespace handling
- Addresses with commas
- Edge cases and error conditions

## Usage Example

```java
AddressParser parser = new AddressParser();

// Parse a valid address
Optional<ParsedAddress> result = parser.parse("Calle 50 # 53-48");
if (result.isPresent()) {
    ParsedAddress addr = result.get();
    System.out.println("Street type: " + addr.getStreetType());      // CALLE
    System.out.println("Primary: " + addr.getPrimaryNumber());       // 50.0
    System.out.println("Secondary: " + addr.getSecondaryNumber());   // 53.0
    System.out.println("Building: " + addr.getBuildingNumber());     // Optional[48]
}

// Parse address with letter and SUR
Optional<ParsedAddress> result2 = parser.parse("CL 12A Sur # 50-20");
if (result2.isPresent()) {
    ParsedAddress addr = result2.get();
    System.out.println("Primary: " + addr.getPrimaryNumber());       // -12.075 (negative due to SUR, A adds 0.075)
    System.out.println("Cardinal: " + addr.getCardinal());           // Optional[SUR]
}
```

## Dependencies
- Java 17
- JUnit Jupiter 5.10.0 (for tests)

## Next Steps
Task 8.2 will implement AddressLocator to convert ParsedAddress objects to geographic Coordinate objects using grid interpolation.

## Notes
- Implementation follows the EXACT algorithm specified in the task requirements
- All test cases from the spec are covered
- Code is production-ready with comprehensive error handling
- Returns Optional<ParsedAddress> for clean error propagation
- Immutable data classes for thread safety
