plugins { kotlin("jvm") }
kotlin {
    jvmToolchain(17)
    sourceSets.getByName("test").kotlin.srcDir("src/check/kotlin")
}
dependencies { testImplementation(kotlin("test-junit")) }
