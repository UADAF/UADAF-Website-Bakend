package config

import com.gt22.uadam.utils.obj
import utils.jsonParser
import java.io.File

val config =  jsonParser.parse(File("config.json").readText()).obj