# user-pics

User actions:
- checkbox click
  - current val = open - propose(v = true)
  - current val = user true - propose(v = false)
  - current val = implied true (blue)

user-pics
    bool
        a
        b
        c
    xors
        red
        v6

xor(r, g, b)
xor(l4, v6, v8)

ops:
    selectBool(a):
        add a to user pics, a = T
    deselectBool(a):
        remove a from user-pics, a = O
    selectXor(r):
        remove(g,b): r = O, g = O
        add(r): r = T