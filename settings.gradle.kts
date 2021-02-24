rootProject.name = "rest-api-flow"

fun defineSubProject(name: String, subdir: String = "") {
    val prjName = "rest-api-flow-$name"
    include(prjName)
    if (subdir.isEmpty()) {
        mkdir(prjName)
        project(":$prjName").projectDir = file(prjName)
    } else {
        val dir = "$subdir/$prjName"
        mkdir(dir)
        project(":$prjName").projectDir = file(dir)
    }
}

defineSubProject("core", "")
defineSubProject("rest", "")
