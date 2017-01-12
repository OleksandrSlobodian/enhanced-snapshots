package com.sungardas.enhancedsnapshots.dto.converter;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.dto.TaskDto;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


public class TaskDtoConverter {

	public static TaskDto convert(TaskEntry task) {
		TaskDto taskDto = new TaskDto();
		BeanUtils.copyProperties(task, taskDto);
		taskDto.setSchedulerTime(task.getSchedulerTime());
		TaskDto.VolumeInfo volumeInfo = new TaskDto.VolumeInfo(task.getVolume(), task.getAvailabilityZone(), task.getInstanceToAttach());
		volumeInfo.volumeId = task.getVolume();
		taskDto.setVolumes(Arrays.asList(volumeInfo));
		return taskDto;
	}

	public static List<TaskEntry> convert(TaskDto taskDto) {
		List<TaskEntry> result = new ArrayList<>();
		for (TaskDto.VolumeInfo volumeInfo : taskDto.getVolumes()) {
			TaskEntry task = copy(taskDto);
			task.setVolume(volumeInfo.volumeId);
			task.setAvailabilityZone(volumeInfo.zone);
			task.setInstanceToAttach(volumeInfo.instanceId);
			result.add(task);
		}
		return result;
	}

	private static TaskEntry copy(TaskDto taskDto){
		TaskEntry task = new TaskEntry();
		BeanUtils.copyProperties(taskDto, task);
		task.setBackupFileName(taskDto.getBackupFileName());
		return task;
	}

	public static List<TaskDto> convert(Iterable<TaskEntry> taskEntries) {
		List<TaskDto> dtos = new ArrayList<>();
		for (TaskEntry task : taskEntries) {
			dtos.add(convert(task));
		}
		return dtos;
	}

	public static List<TaskDto> convert(Collection<TaskEntry>... taskEntries) {
		return Arrays.stream(taskEntries).flatMap(s -> s.stream()).parallel()
				.map(TaskDtoConverter::convert).collect(Collectors.toList());
	}
}
