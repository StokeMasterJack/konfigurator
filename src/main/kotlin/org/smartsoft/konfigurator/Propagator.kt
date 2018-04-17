package org.smartsoft.konfigurator


class Propagator {

    companion object {

        fun propagate(f: ExpFactory, constraint: Complex, pics: Assignment?): Exp {

            require(constraint !is LitAnd)

            if (pics != null) {
                require(pics.isNotEmpty())
                if (pics is LitAnd && pics.isFailed()) return False
            }

            val assignments: MutableLitAnd = MutableLitAnd().apply {
                if (pics != null) {
                    this.assignInPlace(pics)
                }
            }

            val queue: MutableList<Assignment> = mutableListOf<Assignment>().apply {
                if (pics != null) this.add(pics)
            }

            var current: Exp = constraint

            while (true) {
                if (assignments.isFailed()) return False

                val cur: Exp = current

                val after: Exp = when (cur) {
                    is False -> {
                        False
                    }
                    is True -> {
                        True
                    }
                    is Lit -> {
                        assignments.assignInPlace(cur)
                        True
                    }
                    is LitAnd -> {
                        assignments.assignInPlace(cur)
                        True
                    }
                    is MixedAnd -> {
                        assignments.assignInPlace(cur.lits)
                        if (!cur.disjoint) {
                            queue.add(cur.careLits())
                        }
                        cur.constraint
                    }
                    is ComplexAnd -> {
                        check(cur.isNotEmpty())
                        cur
                    }
                    is NonAndComplex -> {
                        cur
                    }
                }

                if (after === False || assignments.isFailed()) return False

                if (after === True || queue.isEmpty()) {
                    return f.mkAndDisjoint(after, assignments)
                }

                check(after is ComplexAnd || after is NonAndComplex)

                val nextLits = queue.removeAt(0)
                current = after.maybeSimplify(nextLits)


            }//end while


        }

    }


}