load("@io_bazel_rules_scala//scala:scala_toolchain.bzl", "scala_toolchain")

scala_toolchain(
    name = "toolchain_impl",
    scalacopts = ["-Ywarn-unused"],
    visibility = ["//visibility:public"]
)

toolchain(
    name = "scala_toolchain",
    toolchain_type = "@io_bazel_rules_scala//scala:toolchain_type",
    toolchain = "toolchain_impl",
    visibility = ["//visibility:public"]
)
