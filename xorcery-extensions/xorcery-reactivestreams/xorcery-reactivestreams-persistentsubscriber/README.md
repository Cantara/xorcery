# Reactive Streams Persistent Subscribers

This module implements persistent subscribers to reactive streams that provide items of type WithMetadata<ArrayNode> and includes a "revision" metadata. The PersistentSubscribersService will set up subscribers to configured streams, delegate incoming events to SPI implementations of PersistentSubscriber, and then write the revision handled to a checkpoint mechanism. If the handling fails the PersistentSubscriber implementation can indicate this which will create an entry in the error log. There is also a recovery mechanism that will be invoked on startup to allow event handling that failed to retry.

The goal here is to achieve at-least-once semantics for handling events from a stream, typically to trigger changes in other systems such as populating a queue or invoking an external API.

## Default SPI implementations
The default filesystem based implementations are named "file".
The checkpointing implementation is pluggable using the PersistentSubscriberCheckpoint and PersistentSubscriberErrorLog SPIs, but the default checkpoint implementation will save the checkpoint in a YAML file in the local filesystem as a string, and the default error log mechanism will save errors (metadata+events+error) in a local YAML file as an array. Recovery is done by manually copying the error items from the error log into the recovery file, and making any necessary changes to the data to allow recovery to work on next startup.

##  Configuration
Here is an example xorcery.yaml configuration (see tests for more examples):
```yaml
persistentsubscribers:
    subscribers:
        - name: testsubscriber
          uri: "ws://mystreamserver:8080"
          stream: "myevents"

          configuration:
            environment: "{{ instance.environment }}"

          checkpointProvider: "file"
          errorLogProvider: "file"
          checkpoint: "{{ instance.home }}/myevents/checkpoint.yaml"
          errors: "{{ instance.home }}/myevents/errors.yaml"
          recovery: "{{ instance.home }}/myevents/recovery.yaml"

```
"name" is the name of the PersistentSubscriber SPI implementation as denoted by either @Named or @Service annotations on the class. "uri" is the base location of the server to connect to. "stream" is the name of the reactive stream to pull events from. "configuration" is the configuration to be sent to the stream server to indicate what events should be included in the stream.

The other settings are specific to the default "file" SPI implementations. Check configured providers for specific settings needed.

You are free to add any custom configuration settings that are needed by the PersistentSubscriber implementation. They can be read on the init(PersistentSubscriberConfiguration) callback on startup.

## Skipping events
Most subscribers will not want to handle all events from a stream. They may want to filter on metadata, such as timestamp, agent, or other custom settings. For this purpose a PersistentSubscriber can provide a filter which will be used to quickly skip through the event stream, and not invoke the subscriber implementation unless needed.

The BasePersistentSubscriber implementation has built-in support for two common cases, which is "skipOld: true" and/or "skipUntil: <timestamp>", which simply ignores all events either until the startup timestamp, or until a given timestamp. This is useful for handling only "new" events in the stream.
