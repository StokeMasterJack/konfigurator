# Ops required by a vehicle configurator
```
Compute var states
    //inputs
    baseConstraint: Exp
    userPicks: Set<Var>

    //computed
    satCount: long

    varStates:Map<Var,VarState>

VarState
    var: Var
    userValue: T ot O
    computedValue:
        inferred-true + fixList
        inferred-false + fixList
        open-dontCare
        open-care

computedValue:
    For checkbox vars:
        for Var v in checkboxVars:
            exp = baseConstraint.condition(userPics.minus(v))
            exp.cond(+v).isSat()
            exp.cond(-v).isSat()
            tt open
            tf implied t - computer fixList
            ft implied f - computer fixList
            ff error

    For radio vars:
        for Var v in radioVars:
            radioGroupVars = v.groupVars
            exp = baseConstraint.condition(userPics.minus(radioGroupVars))
            exp.cond(+v).isSat()
            exp.cond(-v).isSat()
            tt open
            tf implied t - computer fixList
            ft implied f - computer fixList
            ff error

```