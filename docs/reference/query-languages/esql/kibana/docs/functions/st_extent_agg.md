% This is generated by ESQL's AbstractFunctionTestCase. Do not edit it. See ../README.md for how to regenerate it.

### ST EXTENT AGG
Calculate the spatial extent over a field with geometry type. Returns a bounding box for all values of the field.

```esql
FROM airports
| WHERE country == "India"
| STATS extent = ST_EXTENT_AGG(location)
```
