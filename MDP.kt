import java.util.*

// -------------------Semantics
// mdp(Start State):
//   - state(name):
//     - +action name
//   - action(name):
//     - state name > probability
//
// -------------------Syntax
// mdp:
// "mdp(<String>) {
//    (<stateDef>)*
//    (<actionDef>)*
//  }" ;
//
// stateDef:
// "state(<String>) {
//    (+<String>)*
//  }"
//
// actionDef:
// "action(<String>) {
//    (<String> > <Double>)*
//  }"

val model =
    mdp("Start") {
        state("Start") {
            +"Go"
            +"Stay"
        }
        state("End") {

        }
        action("Go") {
            "End" > 1.0
        }
        action("Stay") {
            "Start" > 0.9
            "End" > 0.1
        }
    }


fun main(args: Array<String>)
{
    val start = model.start()

    var input = ""

    while (input != "q")
    {
        print("${start.currentState()} > ")
        input = readLine()!!
        when (input.split(" ")[0])
        {
            "list" -> println(start.getActions())
            "choose" -> start.choose(input.split(" ")[1])
        }
    }
}



fun mdp(startState: String, init: MDP.() -> Unit): MDP
{
    val mdp = MDP(startState)
    mdp.init()

    for (state in mdp.states.map { e -> e.value })
    {
        for (action in state.actions)
        {
            if (action !in mdp.actions.map { e -> e.key })
            {
                error("Action \'$action\' referenced by state \'${state.name}\' was not defined")
            }
        }
    }
    for (action in mdp.actions.map { e -> e.value })
    {
        for (state in action.consequences.map { e -> e.key })
        {
            if (state !in mdp.states.map { e -> e.key })
            {
                error("State \'$state\' referenced by action \'${action.name}\' was not defined")
            }
        }
    }

    if (startState !in mdp.states.keys)
    {
        error("Start state not defined.")
    }
    else
    {
        return mdp
    }
}

class RunningMDP(private val mdp: MDP, private var currentState: State)
{
    var points: Double = 0.0

    fun currentState(): String
    {
        return currentState.name
    }

    fun getActions() = currentState.actions

    fun choose(option: String)
    {
        if (option in currentState.actions)
        {
            if (option in mdp.actions.keys)
            {
                takeAction(mdp.actions[option]!!)
            }
            else
            {
                error("Action is undefined.")
            }
        }
        else
        {
            error("Action $option is not possible given current state ${currentState()}.")
        }
    }

    private fun takeAction(action: MDPAction)
    {
        val actions = action.consequences.entries.toList()
        val probCDF: MutableList<Double> = mutableListOf(actions.first().value)
        actions.map { e -> e.value }
                .reduce { acc, next ->
                    probCDF.add(acc + next)
                    acc + next
                }

        val rand = Random().nextDouble()

        val index = probCDF.takeWhile { acc -> acc < rand }.size

        val nextState = mdp.states[actions[index].key]
        currentState = nextState!!
        points += currentState.points
    }
}

class MDP(private val startState: String)
{
    val states: MutableMap<String, State> = mutableMapOf()
    val actions: MutableMap<String, MDPAction> = mutableMapOf()

    fun state(name: String, pnts: Double = 0.0, init: State.() -> Unit)
    {
        val newState = State(name, pnts)
        newState.init()
        states[name] = newState
    }

    fun action(name: String, init: MDPAction.() -> Unit)
    {
        val newAction = MDPAction(name)
        newAction.init()
        actions[name] = newAction
    }

    fun start() = RunningMDP(this, states[startState]!!)
}

class State(val name: String, val points: Double = 0.0)
{
    val actions: MutableSet<String> = mutableSetOf()

    operator fun String.unaryPlus()
    {
        actions.add(this)
    }
}
class MDPAction(val name: String)
{
    val consequences: MutableMap<String, Double> = mutableMapOf()

    operator fun String.compareTo(prob: Double) : Int
    {
        consequences[this] = prob
        return 0
    }
}