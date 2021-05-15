package cf.thebone.ddctoolbox.api.model

data class AEQSearchResult (val directory_url: String,
                            var web_url: String = "",
                            val model: String,
                            val group: String)