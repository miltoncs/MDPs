
fun main(args: Array<String>)
{
    val grid = GridMDP(4,3)
    grid.block(2,2)

    grid.setRewardFunction { x, y ->
        when(Pair(x,y))
        {
            Pair(4,3) -> 1.0
            Pair(4,2) -> -1.0
            else -> 0.0
        }
    }

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

    fun determineUtilities(gamma: Double, baseReward: Double): Array<Array<Double>>
    {
        val utilities: Array<Array<Double>> = Array(columns) {
            _ -> Array(rows) {
                _ -> baseReward
            }
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

        val maxNeighborUtility: (Int, Int) -> Double = { x, y ->

            val options: MutableList<Double> = mutableListOf()

            if (checkXY(x,y+1)) options.add(utilities[x][y+1])
            if (checkXY(x+1,y)) options.add(utilities[x+1][y])
            if (checkXY(x,y-1)) options.add(utilities[x][y-1])
            if (checkXY(x-1,y)) options.add(utilities[x-1][y])

            options.sorted().first()
        }

        doIterations(10)
        {
            forEachSquare { x, y ->
                utilities[x][y] = rewardFunction.invoke(x,y) + (gamma * maxNeighborUtility(x,y))
            }
        }

        return utilities
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
