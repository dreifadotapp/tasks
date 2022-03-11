package dreifa.app.tasks.executionContext

import dreifa.app.types.UniqueId

data class PipelineContext(val id: UniqueId, val tag: String) {
    companion object {
        val DEFAULT = PipelineContext(id = UniqueId.fromString("000"), tag = "default")
    }
}

