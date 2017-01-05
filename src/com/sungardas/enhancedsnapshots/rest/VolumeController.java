package com.sungardas.enhancedsnapshots.rest;

import java.util.Set;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.sungardas.enhancedsnapshots.dto.VolumeDto;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.service.VolumeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;


@RestController
@RequestMapping("/volume")
public class VolumeController {

    @Autowired
    private VolumeService volumeService;
    @Autowired
    private AWSCommunicationService awsCommunication;

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity getAllVolumes() {
        try {
            Set<VolumeDto> volumes = volumeService.getVolumes();
            return new ResponseEntity(volumes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity("Failed to get volumes.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/{regionId}", method = RequestMethod.GET)
    public ResponseEntity getVolumesByRegion(@PathVariable("regionId") String region) {
        try {
            Set<VolumeDto> volumes = volumeService.getVolumesByRegion(Region.getRegion(Regions.fromName(region)));
            return new ResponseEntity(volumes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity("Failed to get volumes for region: " + region, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/attach", method = RequestMethod.POST)
    public ResponseEntity attachVolumeToInstance(@RequestBody AttachVolumeToInstanceDTO payload) {
        try {
            awsCommunication.attachVolume(awsCommunication.getInstance(payload.instanceId),
                    awsCommunication.getVolume(payload.volumeId));
            return new ResponseEntity("", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity("Failed to attach volume: " + payload.volumeId + " to instance: " + payload.instanceId, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static class AttachVolumeToInstanceDTO {
        public String volumeId;
        public String instanceId;
    }

}
