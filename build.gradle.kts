plugins {
    id("java")
}

group = "net.bjmsw"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
        name = "dv8tion maven"
    }
    jcenter()
    mavenLocal()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.sedmelluq:lavaplayer:1.3.77")

    implementation ("net.dv8tion:JDA:5.0.0-beta.11")
    implementation ("ch.qos.logback:logback-classic:1.2.8")

    // https://mvnrepository.com/artifact/net.iharder/base64
    implementation("net.iharder:base64:2.3.9")

    // https://mvnrepository.com/artifact/org.jdom/jdom2
    implementation("org.jdom:jdom2:2.0.6.1")

    // https://mvnrepository.com/artifact/com.cedarsoftware/json-io
    implementation("com.cedarsoftware:json-io:4.13.0")

    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.11.0")

    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:20230227")

}

tasks.test {
    useJUnitPlatform()
}