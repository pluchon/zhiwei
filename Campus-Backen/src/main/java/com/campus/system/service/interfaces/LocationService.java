package com.campus.system.service.interfaces;

import com.campus.system.dto.BuildingEditDTO;
import com.campus.system.dto.CampusEditDTO;
import com.campus.system.entity.LocationSnapshot;
import com.campus.system.vo.BuildingVO;
import com.campus.system.vo.CampusVO;
import com.campus.system.vo.LocationOptionVO;
import java.util.List;

/**
 * 校区与楼栋基础数据业务接口。
 */
public interface LocationService {

  List<LocationOptionVO> selectionOptions();

  List<CampusVO> adminCampuses();

  List<BuildingVO> adminBuildings(Long campusId);

  CampusVO createCampus(CampusEditDTO body);

  CampusVO updateCampus(Long id, CampusEditDTO body);

  void enableCampus(Long id);

  void disableCampus(Long id);

  void deleteCampus(Long id);

  void restoreCampus(Long id);

  BuildingVO createBuilding(BuildingEditDTO body);

  BuildingVO updateBuilding(Long id, BuildingEditDTO body);

  void enableBuilding(Long id);

  void disableBuilding(Long id);

  void deleteBuilding(Long id);

  void restoreBuilding(Long id);

  LocationSnapshot resolveSnapshot(Long campusId, Long buildingId, String locationDetail);
}
