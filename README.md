# Konfigurator - a product configurator engine


### Overview
This library is designed to provide the back-end logic for an interactive product configurator,
where the user selects various product features thru the UI (say, Red, V6-Engine and Convertible) 
and the framework determines:

- which additional features _must_ be selected based on the current user pics
- which features _cannot_ be selected based on the current user pics
 
This tool allows you to:
 
- Define a set of features. These are represented as boolean variables (VarSet).
- Define a set of constraints (in the form of boolean expressions) on a those variables. 
  1. A VarSet: a set of boolean variables representing the product features.      
  2. A Pics: a set of constraints (in the form of boolean expressions) on a those variables.
- Set user pics, then:
    - Test if the current configuration is valid
    - Compute a new, simplified constraint based on current configuration.
    - _**Compute inferred pics based on user pics. This is the primary purpose of this library**_:
      - Vars that are inferred _True_ represent features that **must** be picked based on current user pics
      - Vars that are inferred _False_ represent features that **cannot** be picked based on current user pics
         

### Defining constraints using Kotlin DSL
A constraint set may be defined using the Kotlin DSL or 
by using the Kotlin API. 
Here is a simple constraint set using the Kotlin DSL:

```kotlin
class SimpleSpace : VarSet() {

    //vars: product features
    val a = +"a"
    val b = +"b"
    val c = +"c"
    val d = +"d"
    val e = +"e"
    val f = +"f"
    val g = +"g"
    val h = +"h"
    val i = +"i"
    val red = +"red"
    val green = +"green"

    //constraints on above vars
    fun mkConstraintSet1() = mkConstraintSet(
        conflict(a, b),
        conflict(a, c),
        requires(a, d),
        requires(d, or(e, f)),
        requires(f, and(g, h, a)),
        xor(red, green),
        requires(green, a))

}
```

### Next, instantiate our VarSet and Pics 

```kotlin
val vars: SimpleSpace = SimpleSpace()
val cs1: ConstraintSet = vars.mkConstraintSet1().print()
```

This produces the following output:

```
User Pics:    []    //no user pics yet
Inferred Pics: []   //nothing can be inferred so far
Unconstrained: [i]  //i is the only var with no constraint
Constraints:        //exactly our original constraint is returned
  Conflict[a, b]
  Conflict[a, c]
  Requires[a, d]
  Requires[d, Or[e, f]]
  Requires[f, And[a, g, h]]
  Xor[green, red]
  Requires[green, a]
```

### Make a user pick: a = true

```kotlin
val cs2 = cs1.assign(vars.a).print()
```

The system computes inferred pics and simplified constraint.
Here is the output:

```
User pics:     [a]          //We now have one user pic
Inferred pics: [d, !b, !c]  //3 vars have been inferred
Unconstrained: [i]          //i is still the only unconstrained var
Constraints:                //Constraint is simplified based on pics
  Or[e, f]
  Requires[f, And[g, h]]
  Xor[green, red]
```

### Make another user pic: f = true
```kotlin
val cs3 = cs2.assign(vars.f).print()
```

Here is the output:

```
User pics:     [a, f]              //We now have 2 user pics
Inferred pics: [d, g, h, !b, !c]   //2 more vars have been inferred
Unconstrained: [i, e]              //we now have 2 unconstrained vars 
Constraints:                       //Constraint is simplified even further
  Xor[green, red]
```  

### Make another user pic: red = true
```kotlin
val cs4 = cs3.assign(vars.red).print()
```

Here is the output:
```
User pics:     [a, f, red]                 //We now have 3 user pics
Inferred pics: [d, g, h, !b, !c, !green]   //And 6 inferred pics
Unconstrained: [i, e]                      //i and e are still unconstrained
Constraints:                               //There is no constraint left
```  

### Make another user pic: i = true
```kotlin
val cs5 = cs4.assign(vars.i).print()
```

The only thing that changed is var i moved 
from _Unconstrained_ to _User pics_:

```
User pics:     [a, f, i, red]
Inferred pics: [d, g, h, !b, !c, !green]
Unconstrained: [e]
Constraints: 
                             
```  

### Make conflicting pic: g = false

Note the use of bang operator in the DSL
```kotlin
val cs6 = cs5.assign(!vars.g).print()
```

Note that the constraint now shows FAILED:

```
User Pics:    [a, f, i, red, !g]
Constraints: FAILED!!
```

### Complete code

Here is the complete code:

```kotlin
val vars: Simple = Simple()
val cs1: ConstraintSet = vars.mkConstraintSet1().print()
val cs2 = cs1.assign(vars.a).print()
val cs3 = cs2.assign(vars.f).print()
val cs4 = cs3.assign(vars.red).print()
val cs5 = cs4.assign(vars.i).print()
val cs6 = cs5.assign(!vars.g).print()
```

### Make things a bit more terse with a bit of Kotlin coolness:

```kotlin
Simple().run {
    mkConstraintSet1().print()
    .assign(a).print()
    .assign(f).print()
    .assign(red).print()
    .assign(i).print()
    .assign(!g).print()
}
```
