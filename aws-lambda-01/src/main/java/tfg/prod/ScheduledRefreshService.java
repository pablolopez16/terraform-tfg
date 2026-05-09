package tfg.prod;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tfg.prod.modules.MergeConfig;
import tfg.prod.services.CalendarMergeService;
import tfg.prod.services.DynamoDbService;
import tfg.prod.services.IcsGeneratorService;

import java.util.List;
import java.util.Map;

@Service
public class ScheduledRefreshService {

    private final DynamoDbService dynamoDb;
    private final CalendarMergeService mergeService;
    private final IcsGeneratorService icsService;
    private final ObjectMapper mapper;
    private final S3Client s3;
    private final String cacheBucket;
    

    private static  ScheduledRefreshService instance;

    @jakarta.annotation.PostConstruct
    public void register() {
        instance = this;
    }

    public static ScheduledRefreshService getInstance() {
        return instance;
    }
    public ScheduledRefreshService(DynamoDbService dynamoDb, CalendarMergeService mergeService,
                                   IcsGeneratorService icsService, ObjectMapper mapper) {
        this.dynamoDb     = dynamoDb;
        this.mergeService = mergeService;
        this.icsService   = icsService;
        this.mapper       = mapper;
        this.s3           = S3Client.builder().httpClient(UrlConnectionHttpClient.create()).build();
        this.cacheBucket  = System.getenv("ICS_CACHE_BUCKET");
    }

    public void refreshAll() {
        List<Map<String, String>> configs = dynamoDb.listMergeConfigs();
        for (Map<String, String> entry : configs) {
            String mergeId = entry.get("merge_id");
            String json    = entry.get("config");
            try {
                MergeConfig config = mapper.readValue(json, MergeConfig.class);
                String ics = icsService.generate(mergeService.merge(config), "Calendario Fusionado");
                s3.putObject(
                    PutObjectRequest.builder()
                        .bucket(cacheBucket)
                        .key("ics/" + mergeId + ".ics")
                        .contentType("text/calendar")
                        .build(),
                    RequestBody.fromString(ics)
                );
                System.out.println("ICS actualizado para mergeId: " + mergeId);
            } catch (Exception e) {
                System.err.println("Error refrescando " + mergeId + ": " + e.getMessage());
            }
        }
    }
}
