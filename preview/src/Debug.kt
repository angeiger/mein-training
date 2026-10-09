import de.meintraining.app.figure.Motions
import de.meintraining.app.figure.solve

fun debugSkeleton(id: String, p: Float) {
    val sk = solve(Motions.all.getValue(id).pose(p))
    println("hip ${sk.hip}\nknee ${sk.knee}\nankle ${sk.ankle}\nhand ${sk.hand}\nelbow ${sk.elbow}")
}
