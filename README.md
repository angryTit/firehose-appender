# LOGBack Appender for Amazon Firehose

Unsynchronised. Use with `ch.qos.logback.classic.AsyncAppender`
    
    <configuration>

    <appender name="FIREHOSE" class="ru.angrytit.FirehoseAppender">
        <encoder>
            <pattern>[%thread] %-5level %logger{35} - %msg %n</pattern>
        </encoder>
        <deliveryStream>test-delivery-stream</deliveryStream>
        <region>us-east-1</region>
        <encoding>UTF-8</encoding>
    </appender>

    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
            <evaluator class="ch.qos.logback.classic.boolex.OnMarkerEvaluator">
                <marker>*DATA*</marker>
            </evaluator>
            <onMismatch>DENY</onMismatch>
            <onMatch>NEUTRAL</onMatch>
        </filter>
        <appender-ref ref="FIREHOSE"/>
        <queueSize>1000</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <maxFlushTime>10000</maxFlushTime>
    </appender>

    <root level="INFO">
        <appender-ref ref="ASYNC"/>
    </root>

    </configuration>
