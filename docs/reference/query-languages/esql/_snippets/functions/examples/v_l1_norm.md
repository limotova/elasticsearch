% This is generated by ESQL's AbstractFunctionTestCase. Do not edit it. See ../README.md for how to regenerate it.

**Example**

```esql
 from colors
 | eval similarity = v_l1_norm(rgb_vector, [0, 255, 255])
 | sort similarity desc, color asc
```

| color:text | similarity:double |
| --- | --- |
| red | 765.0 |
| crimson | 650.0 |
| maroon | 638.0 |
| firebrick | 620.0 |
| orange | 600.0 |
| tomato | 595.0 |
| brown | 591.0 |
| chocolate | 585.0 |
| coral | 558.0 |
| gold | 550.0 |


