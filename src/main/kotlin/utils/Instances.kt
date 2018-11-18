package utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

val jsonParser by lazy { JsonParser() }
val gson by lazy { GsonBuilder().setPrettyPrinting().create() }
