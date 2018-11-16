package config

import com.gt22.uadam.utils.obj
import utils.Instances
import java.io.File

val config = Instances.jsonParser.parse(File("config.json").readText()).obj