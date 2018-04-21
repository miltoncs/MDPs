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
        print("${start.currentState} > ")
        input = readLine()!!
        when (input.split(" ")[0])
        {
            "list" -> println(start.actions)
            "choose" -> start.choose(input.split(" ")[1])
        }
    }
}



fun mdp(startState: String, init: MDP.() -> Unit): MDP
{
    val mdp = MDP(startState)
    mdp.init()

    // If any state is missing an action, error.
    val definedActions = mdp.actions.map { it.key }
    val undefinedActions = mdp.states.flatMap { it.value.actions }
            .filter { it !in definedActions }
            .toList()
    if (undefinedActions.isNotEmpty()) error("The following actions are missing: $undefinedActions")

    // If any action's consequence is not missing it's state, error.
    val definedConsequences = mdp.actions.map { it.value }.flatMap { it.consequences.map { it.key } }
    val undefinedStates = definedConsequences.filter { it !in mdp.states.map { it.key }}
    if (undefinedStates.isNotEmpty()) error("The following states must be defined $undefinedStates")

    if (startState !in mdp.states.keys) error("Start state not defined.")
    return mdp
}

class RunningMDP(private val mdp: MDP, currentState: State)
{
    var currentState: State = currentState
        private set

    var points: Double = 0.0

    val actions: Set<String> get() = currentState.actions

    fun choose(option: String)
    {
        if (option !in actions) error("Action $option is not possible given current state $currentState.")
        if (option !in mdp.actions.keys) error("Action is undefined.")
        takeAction(mdp.actions[option]!!)
    }

    private fun takeAction(action: Action)
    {
        val actions = action.consequences.entries.toList()
        val probCDF: MutableList<Double> = mutableListOf(actions.first().value)
        actions.map { it.value }
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
    val actions: MutableMap<String, Action> = mutableMapOf()

    fun state(name: String, points: Double = 0.0, init: State.() -> Unit)
    {
        val newState = State(name, points)
        newState.init()
        states[name] = newState
    }

    fun action(name: String, init: Action.() -> Unit)
    {
        val newAction = Action(name)
        newAction.init()
        actions[name] = newAction
    }

    fun start() = RunningMDP(this, states[startState]!!)
}

class State(val name: String, val points: Double = 0.0)
{
    val actions: MutableSet<String> = mutableSetOf()

    operator fun String.unaryPlus() = actions.add(this)
}
class Action(val name: String)
{
    val consequences: MutableMap<String, Double> = mutableMapOf()

    operator fun String.compareTo(prob: Double) : Int
    {
        consequences[this] = prob
        return 0
    }
}