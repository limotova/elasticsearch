<!--
This is generated by ESQL’s AbstractFunctionTestCase. Do no edit it. See ../README.md for how to regenerate it.
-->

### ASIN
Returns the arcsine of the input
numeric expression as an angle, expressed in radians.

```
ROW a=.9
| EVAL asin=ASIN(a)
```
