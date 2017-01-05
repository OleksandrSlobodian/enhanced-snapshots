package com.sungardas.enhancedsnapshots.rest;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.SnsRuleEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    //TODO move to db
    private String snsTopic = "";
    private Map<String, SnsRuleEntry> rules = new HashMap<>();

    @RolesAllowed("ROLE_ADMIN")
    @RequestMapping(value = "/sns/settings", method = RequestMethod.GET)
    public ResponseEntity getSnsSettings() {
        SnsSettingsDto dto = new SnsSettingsDto();
        dto.topic = snsTopic;
        return new ResponseEntity(dto, HttpStatus.OK);
    }

    @RolesAllowed("ROLE_ADMIN")
    @RequestMapping(value = "/sns/settings", method = RequestMethod.PUT)
    public ResponseEntity setSnsSettings(@RequestBody SnsSettingsDto settingsDto) {
        snsTopic = settingsDto.topic;
        return new ResponseEntity(HttpStatus.OK);
    }

    @RolesAllowed("ROLE_ADMIN")
    @RequestMapping(value = "/sns/rule/operation", method = RequestMethod.GET)
    public ResponseEntity getSnsRuleOperation() {
        return new ResponseEntity(snsRuleOperationsDto, HttpStatus.OK);
    }

    @RolesAllowed("ROLE_ADMIN")
    @RequestMapping(value = "/sns/rule/status", method = RequestMethod.GET)
    public ResponseEntity getSnsRuleStatuses() {
        return new ResponseEntity(snsRuleStatusesDto, HttpStatus.OK);
    }

    @RolesAllowed("ROLE_ADMIN")
    @RequestMapping(value = "/sns/rule", method = RequestMethod.GET)
    public ResponseEntity getRules() {
        return new ResponseEntity(rules.values(), HttpStatus.OK);
    }

    @RolesAllowed("ROLE_ADMIN")
    @RequestMapping(value = "/sns/rule", method = RequestMethod.POST)
    public ResponseEntity addRule(@RequestBody SnsRuleEntry snsRuleEntry) {
        snsRuleEntry.setId(Math.random()+"");
        rules.put(snsRuleEntry.getId(), snsRuleEntry);
        return new ResponseEntity(HttpStatus.OK);
    }

    @RolesAllowed("ROLE_ADMIN")
    @RequestMapping(value = "/sns/rule", method = RequestMethod.PUT)
    public ResponseEntity updateRule(@RequestBody SnsRuleEntry snsRuleEntry) {
        if(rules.containsKey(snsRuleEntry.getId())) {
            rules.put(snsRuleEntry.getId(), snsRuleEntry);
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity("Rule not found", HttpStatus.NOT_FOUND);
        }
    }

    @RolesAllowed("ROLE_ADMIN")
    @RequestMapping(value = "/sns/rule/{ruleId}", method = RequestMethod.DELETE)
    public ResponseEntity addRule(@PathVariable("ruleId") String ruleId) {
        if(rules.containsKey(ruleId)) {
            rules.remove(ruleId);
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity("Rule not found", HttpStatus.NOT_FOUND);
        }
    }


    //Constants
    private static final SnsRuleOperationsDto snsRuleOperationsDto = new SnsRuleOperationsDto(
            Arrays.asList(
                    TaskEntry.TaskEntryType.BACKUP,
                    TaskEntry.TaskEntryType.RESTORE
            )
    );

    private static final SnsRuleStatusesDto snsRuleStatusesDto = new SnsRuleStatusesDto(
            Arrays.asList(
                    TaskEntry.TaskEntryStatus.COMPLETE,
                    TaskEntry.TaskEntryStatus.ERROR
            )
    );


    //DTOs
    private class SnsSettingsDto {
        public String topic;
    }

    private static final class SnsRuleOperationsDto {
        public final List<TaskEntry.TaskEntryType> operations;

        public SnsRuleOperationsDto(List<TaskEntry.TaskEntryType> operations) {
            this.operations = operations;
        }
    }

    private static final class SnsRuleStatusesDto {
        public final List<TaskEntry.TaskEntryStatus> statuses;

        public SnsRuleStatusesDto(List<TaskEntry.TaskEntryStatus> statuses) {
            this.statuses = statuses;
        }
    }
}
