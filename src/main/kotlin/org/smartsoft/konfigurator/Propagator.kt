package org.smartsoft.konfigurator


class Propagator {

    companion object {

        /**
         * conjoin and simplify
         */
        fun propagate(f: VarSpace, constraint: Complex, pics: Assignment?): Exp {

            require(constraint is ComplexAnd || constraint is MixedAnd || constraint is NonAnd)

            val assignments = LitAndBuilder(pics)

            val queue: MutableList<Assignment> = mutableListOf<Assignment>().apply {
                if (pics != null && !pics.isEmpty()) this.add(pics)
            }

            var current: Exp = constraint

            while (true) {
                if (assignments.isFailed()) return f.mkFalse()

                val cur: Exp = current

                val after: Exp = when (cur) {
                    is False -> {
                        f.mkFalse()
                    }
                    is True -> {
                        f.mkTrue()
                    }
                    is Lit -> {
                        assignments.assign(cur)
                        f.mkTrue()
                    }
                    is LitAnd -> {
                        assignments.assign(cur)
                        f.mkTrue()
                    }
                    is MixedAnd -> {
                        assignments.assign(cur.lits)
                        if (!cur.disjoint) {
                            queue.add(cur.overlapLits())
                        }
                        cur.constraint
                    }
                    is ComplexAnd -> {
                        check(cur.isNotEmpty())
                        cur
                    }
                    is NonAnd -> {
                        cur
                    }
                }

                if (after is False || assignments.isFailed()) return f.mkFalse()

                if (after is True || queue.isEmpty()) {
                    return f.mkAndDisjoint(after, assignments.mk())
                }

                check(after is ComplexAnd || after is NonAnd)

                val nextLits = queue.removeAt(0)
                current = after.maybeSimplify(nextLits, f)


            }//end while


        }

    }


}