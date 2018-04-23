fun main(args: Array<String>)
{
    val grid = GridMDP(4,3)
    grid.block(2,2)

    grid.setRewardFunction { x, y ->
        when(Pair(x,y))
        {
            Pair(4,3) -> 1.0
            Pair(4,2) -> -1.0
            else -> -0.04
        }
    }

    grid.determineUtilities(10, 1.0, true)

    val agent = grid.getAgent()

    agent.getPosition()

    agent.moveUp()
    agent.moveRight()
    agent.moveDown()
    agent.moveLeft()
}

class GridMDP(val columns: Int, val rows: Int)
{
    private val blocked: MutableList<Pair<Int, Int>> = mutableListOf()
    private var rewardFunction: (Int, Int) -> Double = {_, _ -> 0.0}

    fun getAgent(): Agent = Agent(this)

    fun block(x: Int, y: Int)
    {
        blocked.add(Pair(x,y))
    }

    fun setRewardFunction(newFun: (Int, Int) -> Double)
    {
        rewardFunction = newFun
    }

    private fun checkXY(newX: Int, newY: Int): Boolean = when {
        newY < 0 || newY > rows -> false
        newX < 0 || newX > columns -> false
        blocked.contains(Pair(newX, newY)) -> false
        else -> true
    }

    fun determineUtilities(iterations: Int, gamma: Double, printSteps: Boolean = false): Array<Array<Double>>
    {
        val utilities: Array<Array<Double>> = Array(columns) {
            _ -> Array(rows) {
                _ -> 0.0
            } // utilities[row_num or y][col_num or x]
        }

        val doIterations: (Int, () -> Unit) -> Unit = { i, chunk ->
            for (iter in 1..i) {
                chunk.invoke()
            }
        }

        val forEachSquare: ((Int, Int) -> Unit) -> Unit = { chunk ->
            for (x in 1..columns)
            {
                for(y in 1..rows)
                {
                    chunk.invoke(x,y)
                }
            }
        }

        val consequenceUtility: (ProbabilisticConsequence) -> Double =
                { it.prob * utilities[it.xy.second][it.xy.first]}

        val actionUtility: (Action, Int, Int) -> Double = { a, x, y ->
            consequencesOf(x, y, a).sumByDouble(consequenceUtility)
        }

        val maxActionUtility: (Int, Int) -> Double = { x, y ->

            availableActions()
                    .map{ a -> actionUtility(a, x, y) }
                    .sorted()
                    .first()
        }

        doIterations(iterations)
        {
            forEachSquare { x, y ->
                utilities[y][x] = rewardFunction(x,y) + (gamma * maxActionUtility(x,y))
                if (printSteps) printUtilities(utilities)
            }
        }

        return utilities
    }

    private fun printUtilities(utilities: Array<Array<Double>>)
    {
        for (row in utilities)
        {
            println(row)
        }
    }

    private fun consequencesOf(x: Int, y: Int, action: Action): Set<ProbabilisticConsequence> =
        mutableSetOf(
                ProbabilisticConsequence(resultOfAction(x,y, action), 0.8),
                ProbabilisticConsequence(resultOfAction(x,y, action+1), 0.1),
                ProbabilisticConsequence(resultOfAction(x,y, action-1), 0.1))


    private fun resultOfAction(x: Int, y: Int, action: Action): Pair<Int, Int> = when (action)
    {
        Action.LEFT -> if (checkXY(x-1, y)) Pair(x-1,y) else Pair(x,y)
        Action.DOWN -> if (checkXY(x, y-1)) Pair(x,y-1) else Pair(x,y)
        Action.RIGHT -> if (checkXY(x+1, y)) Pair(x+1, y) else Pair(x,y)
        Action.UP -> if (checkXY(x, y+1)) Pair(x, y+1) else Pair(x,y)
    }


    private fun availableActions() = Action.values()

    data class ProbabilisticConsequence(val xy: Pair<Int, Int>, val prob: Double)

    enum class Action
    {
        UP, RIGHT, DOWN, LEFT;

        fun next() = when(this)
        {
            UP -> RIGHT
            RIGHT -> DOWN
            DOWN -> LEFT
            LEFT -> UP
        }

        fun prev() = when(this)
        {
            UP -> LEFT
            LEFT -> DOWN
            DOWN -> RIGHT
            RIGHT -> UP
        }

        operator fun plus(n: Int): Action = when
        {
            n > 0 -> this.next() + 1
            n < 0 -> this.prev() - 1
            else -> this // assuming n == 0
        }

        operator fun minus(n: Int): Action = this + (-n)
        operator fun unaryMinus(): Action = this.next().next()

    }

    class Agent(val grid: GridMDP)
    {

        var x = 1 // column

        var y = 1 // row
        fun getPosition() = Pair(x,y)
        fun moveUp() = if (grid.checkXY(x, y+1)) ++y else y
        fun moveDown() = if (grid.checkXY(x,y-1)) --y else y
        fun moveRight() = if (grid.checkXY(x+1, y)) ++x else x
        fun moveLeft() = if (grid.checkXY(x+1, y)) --x else x

    }
}
