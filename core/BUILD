scala_library(
    name = "fibers_core",
    srcs = glob(["lib/**/*.scala"]),
    deps = ["@deps//:compile"],
    plugins = ["@org_scala_lang_plugins_scala_continuations_plugin_2_12_2//jar"],
    scalacopts = ["-Ywarn-unused", "-P:continuations:enable"],
    visibility = ["//visibility:public"],
)
