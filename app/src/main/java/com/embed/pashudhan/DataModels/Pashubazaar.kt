package com.embed.pashudhan.DataModels

data class Pashubazaar (
    var id: String? = null,
    var animalAge: String? = null,
    var animalBreed: String? = null,
    var animalByaat: String? = null,
    var animalImages: ArrayList<String>? = null,
    var animalMilkCapacity: String? = null,
    var animalMilkQuantity: String? = null,
    var animalPrice: String? = null,
    var animalType: String? = null,
    var user_uuid: String? = null,
    var location: ArrayList<String>? = null,
    var name: String? = null,
    var favouritesOf: ArrayList<String>? = null,
    var timestamp: String? = null,
)
