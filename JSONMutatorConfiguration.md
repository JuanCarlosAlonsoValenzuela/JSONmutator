## Mutation operators used

This document specifies the mutation operators used for our experiments:

1. **long.replace:** Operator that mutates a long by completely replacing it.
1. **long.mutate:** Operator that mutates a long by adding or subtracting a delta number to the original number.
1. **double.replace:** Operator that mutates a double by completely replacing it.
1. **double.mutate:** Operator that mutates a double by adding or subtracting a delta number to the original number
1. **string.replace:** Operator that mutates a string by completely replacing it.
1. **string.addSpecialCharacters:** Operator that mutates a string by adding special characters like "/", "*", and ",".
1. **string.mutate:** Operator that mutates a string by adding, removing or replacing one single character.
1. **string.boundary:** Operator that mutates a string by replacing it with a boundary value, namely an empty string, an uppercase string, a lowercase string, a string of minimum length or a string of maximum length.
1. **boolean.mutate:** Operator that mutates a boolean by inverting its value
1. **array.removeElement:** Operator that mutates an array by removing a number of elements from it.
1. **array.empty:** Operator that converts an array to empty by removing all elements from it.
1. **array.disorderElements:** Operator that mutates an array by disordering the elements in it.