load("@graknlabs_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")

java_library(
    name = "grakn",
    srcs = glob([
        "*.java",
        "action/*.java",
        "action/read/*.java",
        "action/write/*.java",
        "action/insight/*.java",
        "driver/*.java",
        "yaml_tool/*.java",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//common:simulation-common",
        "//common/action",
        "//common/driver",
        "//common/utils",
        "//common/world",
        "//common/yaml_tool",
        "//config",
        "@graknlabs_grabl_tracing//client",
        "@neo4j//:io_grakn_client_grakn_client",
        "@neo4j//:io_graql_graql_lang",
        "@maven//:com_google_guava_guava",
        "@maven//:org_apache_commons_commons_csv",
        "@maven//:org_slf4j_slf4j_api",
    ],
)

checkstyle_test(
    name = "checkstyle",
    targets = [":grakn"],
    license_type = "agpl"
)

