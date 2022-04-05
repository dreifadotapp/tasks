package dreifa.app.tasks.opentelemtry

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

class InMemorySpanExporter : SpanExporter {
    val allSpans = ArrayList<SpanData>()
    override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
        allSpans.addAll(spans)
        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode {
        allSpans.clear()
        return CompletableResultCode.ofSuccess()
    }

    override fun shutdown(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }
}