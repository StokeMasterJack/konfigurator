# Darwiche Compilers vs Dave Compilers

## Darwiche:
- Has both top-down and bottom-up
- Takes cnf

## Dave:
- Has only top-down
- Takes input as list (one big and) of constraints
  where each constraint is on PL

## Conclusion
- The amount of time needed to _flatten_ our configurator constraints into
CNF is substantial.
- The generated CNF is much harder to compile.
- It is not worth it to convert the rules to CNF.
- Better off using the Dave PL-to-DNNF compiler