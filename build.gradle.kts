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
    maven(url = "https://maven.lavalink.dev/releases")
    jcenter()
    mavenLocal()
    maven {
        url = uri("https://jitpack.io")
    } // For com.github.walkyst.JAADec-fork:jaadec-ext-aac & ibxm-fork:com.github.walkyst:ibxm-fork
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("dev.arbjerg:lavaplayer:2.2.2")

    implementation("dev.lavalink.youtube:common:1.10.2")

    implementation ("net.dv8tion:JDA:5.2.1")
    implementation ("ch.qos.logback:logback-classic:1.2.8")

    // https://mvnrepository.com/artifact/net.iharder/base64
    implementation("net.iharder:base64:2.3.9")

    // https://mvnrepository.com/artifact/org.jdom/jdom2
    implementation("org.jdom:jdom2:2.0.6.1")

    // https://mvnrepository.com/artifact/com.cedarsoftware/json-io
    implementation("com.cedarsoftware:json-io:4.13.0")

    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.15.0")

    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:20231013")



}

tasks.test {
    useJUnitPlatform()
}