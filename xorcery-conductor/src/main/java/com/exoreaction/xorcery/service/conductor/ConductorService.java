package com.exoreaction.xorcery.service.conductor;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.VariableResolver;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Group;
import com.exoreaction.xorcery.service.conductor.api.GroupTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */
@Service
@MessageReceiver
@Named(ConductorService.SERVICE_TYPE)
public class ConductorService
        implements PreDestroy {

    public static final String SERVICE_TYPE = "conductor";

    private Logger logger = LogManager.getLogger(getClass());

    private ServiceResourceObject sro;
    private Topic<ServiceResourceObject> registryTopic;
    private Configuration configuration;
    private ConductorConfiguration conductorConfiguration;

    private final Queue<ServiceResourceObject> serviceResourceObjectQueue = new ArrayBlockingQueue<>(1024);
    private ScheduledExecutorService resourceProcessor;

    private final GroupTemplates groupTemplates;
    private final Groups groups;

    @Inject
    public ConductorService(Topic<ServiceResourceObject> registryTopic,
                            Topic<GroupTemplate> groupTemplateTopic,
                            Topic<Group> groupTopic,
                            Configuration configuration) {
        this.registryTopic = registryTopic;
        this.configuration = configuration;
        this.conductorConfiguration = new ConductorConfiguration(configuration.getConfiguration("conductor"));

        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        groups = new Groups(new Groups.GroupsListener() {

            @Override
            public void addedGroup(Group group) {
                groupTopic.publish(group);
                try {
                    logger.debug("Added group:\n" + mapper.writeValueAsString(group.resourceObject().object()));
                } catch (JsonProcessingException e) {
                    // Ignore
                }
            }

            @Override
            public void updatedGroup(Group group) {
                groupTopic.publish(group);
                try {
                    logger.debug("Updated group:\n" + mapper.writeValueAsString(group.resourceObject().object()));
                } catch (JsonProcessingException e) {
                    // Ignore
                }
            }
        });
        groupTemplates = new GroupTemplates(new GroupTemplates.GroupTemplatesListener() {
            @Override
            public void addedTemplate(GroupTemplate groupTemplate) {
                groupTemplateTopic.publish(groupTemplate);
                try {
                    logger.debug("Added template:\n" + mapper.writeValueAsString(groupTemplate.resourceObject().object()));
                } catch (JsonProcessingException e) {
                    // Ignore
                }
            }
        }, groups);


        sro = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .api("conductor", "api/conductor")
                .websocket("conductorgroups", "ws/conductor/groups")
                .build();

        loadTemplates();

/*
        sro.getLinkByRel("conductorgroups").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new ConductorPublisher(), ConductorPublisher.class);
        });
*/

//        registry.addRegistryListener(new ConductorRegistryListener());

    }

    protected void loadTemplates() {
        // Load templates
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        for (JsonNode templateJson : conductorConfiguration.getTemplates()) {

            ObjectNode templateNode = null;
            if (templateJson.isTextual()) {
                String templateName = templateJson.textValue();
                logger.info("Loading conductor template from:" + templateName);
                try {
                    URI templateUri = URI.create(templateName);
                    templateNode = (ObjectNode) objectMapper.readTree(templateUri.toURL().openStream());

                } catch (IllegalArgumentException | IOException e) {
                    // Just load from classpath
                    try (InputStream in = ClassLoader.getSystemResourceAsStream(templateName)) {
                        if (in == null) {
                            logger.error("Could not find template " + templateName);
                        } else {
                            templateNode = (ObjectNode) objectMapper.readTree(in);
                        }
                    } catch (IOException ex) {
                        logger.error("Could not load template " + templateName, ex);
                    }
                }

                if (templateNode != null) {
                    templateNode = new VariableResolver().apply(configuration.json(), templateNode);
                    ResourceDocument templates = new ResourceDocument(templateNode);
                    templates.getResources().ifPresent(ros -> ros.forEach(ro -> addTemplate(new GroupTemplate(ro))));
                    templates.getResource().ifPresent(ro -> addTemplate(new GroupTemplate(ro)));
                }
            } else if (templateJson.isObject()) {
                templateNode = (ObjectNode) templateJson;

                templateNode = new VariableResolver().apply(configuration.json(), templateNode);
                ResourceObject template = new ResourceObject(templateNode);
                addTemplate(new GroupTemplate(template));
            }
        }
    }

    public void addTemplate(GroupTemplate groupTemplate) {
        groupTemplates.addTemplate(groupTemplate);
    }

    public void removeTemplate(String templateId) {
//        groupTemplates.removeIf(t -> t.resourceObject().getId().equals(templateId));
    }

    public GroupTemplates getGroupTemplates() {
        return groupTemplates;
    }

    public Groups getGroups() {
        return groups;
    }

    // Start processing of ServiceResourceObjects after startup of Xorcery
    public void addedService(@SubscribeTo ServiceResourceObject service) {
        serviceResourceObjectQueue.add(service);
    }

    public void startResourceProcessing() {
        registryTopic.publish(sro);
        resourceProcessor = Executors.newSingleThreadScheduledExecutor();
        processResources(); // Do this synchronously on startup first, so that after Xorcery has started all groups have been created and processed
        resourceProcessor.submit(this::processResources);
    }

    protected void processResources() {
        if (!resourceProcessor.isShutdown()) {
            ServiceResourceObject sro;
            while ((sro = serviceResourceObjectQueue.poll()) != null) {
                groupTemplates.addedService(sro);
            }
            resourceProcessor.schedule(this::processResources, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void preDestroy() {
        if (resourceProcessor != null) {
            resourceProcessor.shutdown();
        }
    }
}
