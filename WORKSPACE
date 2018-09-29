scala_rules_version="41ac5be57e50ee5433fa40e6a1de86914d92dbfb"
maven_rules_version="9c3b07a6d9b195a1192aea3cd78afd1f66c80710"

http_archive(
    name = "io_bazel_rules_scala",
    url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip" % scala_rules_version,
    type = "zip",
    strip_prefix= "rules_scala-%s" % scala_rules_version,
)

http_archive(
    name = "org_pubref_rules_maven",
    url = "https://github.com/pubref/rules_maven/archive/%s.zip" % maven_rules_version,
    type = "zip",
    strip_prefix= "rules_maven-%s" % maven_rules_version,
)

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories", "scala_mvn_artifact")
scala_repositories()

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")
register_toolchains("//toolchains:scala_toolchain")

load("@org_pubref_rules_maven//maven:rules.bzl", "maven_repositories", "maven_repository")
maven_repositories()

maven_repository(
    name = "deps",
    repositories = {
    },
    omit = [
        "org.scala-lang:scala-library",
    ],
    force = [
        "org.scala-lang:scala-library:2.12.7",
        "org.scala-lang:scala-reflect:2.12.7",
        "org.scala-lang:scala-compiler:2.12.7",
    ],
    deps = [
        "org.scala-lang.plugins:scala-continuations-library_2.12:1.0.3",
        "org.scala-lang.plugins:scala-continuations-plugin_2.12.2:1.0.3",

        "io.monix:monix-reactive_2.12:2.3.3",
    ],
    transitive_deps = [
        '2b5a0a9c06db69365a916317d77d068f2b9185d0:io.monix:monix-eval_2.12:2.3.3',
        '700016fcc15ffef9a83dfd082fc88e25d936275a:io.monix:monix-execution_2.12:2.3.3',
        '82e593b89260a562f9ac334a290a37f5527a474c:io.monix:monix-reactive_2.12:2.3.3',
        '007df159e73f74ca04f0330d350d85fadb3e1d9d:io.monix:monix-types_2.12:2.3.3',
        '1d055e97b997dae4d8ea28ae5ebc328334f82ac6:org.jctools:jctools-core:2.0.1',
        '14b8c877d98005ba3941c9257cfe09f6ed0e0d74:org.reactivestreams:reactive-streams:1.0.0',
        'e22de3366a698a9f744106fb6dda4335838cf6a7:org.scala-lang.modules:scala-xml_2.12:1.0.6',
        'fc5af375ef8c9da08fd5dc6c8055aeec09be60ae:org.scala-lang.plugins:scala-continuations-library_2.12:1.0.3',
        '88dd2cf0cb6f74ed02c19872b92d64e09050cff2:org.scala-lang.plugins:scala-continuations-plugin_2.12.2:1.0.3',
        'a393b72134dff0751d5110c3e4808689deba75c1:org.scala-lang:scala-compiler:2.12.7',
        'omit:org.scala-lang:scala-library:2.12.7',
        'c5a8eb12969e77db4c0dd785c104b59d226b8265:org.scala-lang:scala-reflect:2.12.7',
    ],
)
load("@deps//:rules.bzl", "deps_compile")
deps_compile()
