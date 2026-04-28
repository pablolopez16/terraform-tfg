package tfg.prod.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tfg.prod.modules.MergeConfig;
import tfg.prod.modules.MergedEvent;
import tfg.prod.services.CalendarMergeService;
import tfg.prod.services.DynamoDbService;
import tfg.prod.services.IcsGeneratorService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/merge")
public class MergeController {

    private final CalendarMergeService mergeService;
    private final IcsGeneratorService  icsService;
    private final DynamoDbService      dynamoDb;
    private final ObjectMapper         mapper;

    public MergeController(CalendarMergeService mergeService, IcsGeneratorService icsService,
                           DynamoDbService dynamoDb, ObjectMapper mapper) {
        this.mergeService = mergeService;
        this.icsService   = icsService;
        this.dynamoDb     = dynamoDb;
        this.mapper       = mapper;
    }

    // Crear configuración de fusión → devuelve mergeId y URL del ICS
    @PostMapping
    public ResponseEntity<?> create(@RequestBody MergeConfig config) {
        if (config.getSources() == null || config.getSources().isEmpty())
            return ResponseEntity.badRequest().body("Se requiere al menos una fuente");
        try {
            String mergeId = UUID.randomUUID().toString();
            config.setMergeId(mergeId);
            dynamoDb.saveMergeConfig(mergeId, mapper.writeValueAsString(config));
            return ResponseEntity.ok(Map.of(
                    "merge_id", mergeId,
                    "ics_url",  "/merge/" + mergeId + "/ics"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // Descargar/suscribir el ICS fusionado
    @GetMapping("/{mergeId}/ics")
    public ResponseEntity<String> getIcs(@PathVariable String mergeId) {
        try {
            String json = dynamoDb.getMergeConfig(mergeId);
            if (json == null) return ResponseEntity.notFound().build();
            MergeConfig config      = mapper.readValue(json, MergeConfig.class);
            List<MergedEvent> events = mergeService.merge(config);
            String ics              = icsService.generate(events, "Calendario Fusionado");
            return ResponseEntity.ok()
                    .header("Content-Type", "text/calendar; charset=UTF-8")
                    .header("Content-Disposition", "attachment; filename=\"calendar.ics\"")
                    .body(ics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{mergeId}")
    public ResponseEntity<?> get(@PathVariable String mergeId) {
        String json = dynamoDb.getMergeConfig(mergeId);
        return json != null ? ResponseEntity.ok(json) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{mergeId}")
    public ResponseEntity<?> delete(@PathVariable String mergeId) {
        dynamoDb.deleteMergeConfig(mergeId);
        return ResponseEntity.ok(Map.of("deleted", mergeId));
    }
}