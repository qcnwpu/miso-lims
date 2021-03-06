package uk.ac.bbsrc.tgac.miso.core.data.impl;

import static uk.ac.bbsrc.tgac.miso.core.util.LimsUtils.nullifyStringIfBlank;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import uk.ac.bbsrc.tgac.miso.core.data.DetailedQcStatus;
import uk.ac.bbsrc.tgac.miso.core.data.DetailedSample;
import uk.ac.bbsrc.tgac.miso.core.data.GroupIdentifiable;
import uk.ac.bbsrc.tgac.miso.core.data.SampleClass;
import uk.ac.bbsrc.tgac.miso.core.data.Subproject;
import uk.ac.bbsrc.tgac.miso.core.data.impl.transfer.TransferItem;
import uk.ac.bbsrc.tgac.miso.core.data.impl.view.SampleHierarchyView;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;

@Entity
@Table(name = "DetailedSample")
@Inheritance(strategy = InheritanceType.JOINED)
public class DetailedSampleImpl extends SampleImpl implements DetailedSample {

  private static final long serialVersionUID = 1L;

  @ManyToOne(optional = true, targetEntity = DetailedSampleImpl.class, fetch = FetchType.LAZY)
  @JoinColumn(name = "parentId", nullable = true)
  private DetailedSample parent;

  @OneToMany(targetEntity = DetailedSampleImpl.class, mappedBy = "parent")
  private List<DetailedSample> children = new ArrayList<>();

  @ManyToOne(targetEntity = SampleClassImpl.class)
  @JoinColumn(name = "sampleClassId", nullable = false)
  private SampleClass sampleClass;

  @ManyToOne(targetEntity = DetailedQcStatusImpl.class)
  @JoinColumn(name = "detailedQcStatusId")
  private DetailedQcStatus detailedQcStatus;
  private String detailedQcStatusNote;

  @ManyToOne(targetEntity = SubprojectImpl.class)
  @JoinColumn(name = "subprojectId")
  private Subproject subproject;

  @Column(nullable = false)
  private Boolean archived = Boolean.FALSE;

  private Integer siblingNumber;

  private String groupId;
  private String groupDescription;
  private boolean isSynthetic = false;

  @Column(nullable = false)
  private boolean nonStandardAlias = false;
  
  @Column(updatable = false)
  private Long preMigrationId;

  @Temporal(TemporalType.DATE)
  private Date creationDate;

  private BigDecimal volumeUsed;
  private BigDecimal ngUsed;

  @OneToOne
  @JoinColumn(name = "sampleId")
  private SampleHierarchyView hierarchyAttributes;

  @Transient
  private Long identityId;

  @Override
  public DetailedSample getParent() {
    return parent;
  }

  @Override
  public void setParent(DetailedSample parent) {
    this.parent = parent;
  }

  @Override
  public List<DetailedSample> getChildren() {
    return children;
  }

  @Override
  public void setChildren(List<DetailedSample> children) {
    this.children = children;
  }

  @Override
  public SampleClass getSampleClass() {
    return sampleClass;
  }

  @Override
  public void setSampleClass(SampleClass sampleClass) {
    this.sampleClass = sampleClass;
  }

  @Override
  public Subproject getSubproject() {
    return subproject;
  }

  @Override
  public void setSubproject(Subproject subproject) {
    this.subproject = subproject;
  }

  @Override
  public Boolean getArchived() {
    return archived;
  }

  @Override
  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  @Override
  public DetailedQcStatus getDetailedQcStatus() {
    return detailedQcStatus;
  }

  @Override
  public void setDetailedQcStatus(DetailedQcStatus detailedQcStatus) {
    this.detailedQcStatus = detailedQcStatus;
  }

  @Override
  public Integer getSiblingNumber() {
    return siblingNumber;
  }

  @Override
  public void setSiblingNumber(Integer siblingNumber) {
    this.siblingNumber = siblingNumber;
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  @Override
  public void setGroupId(String groupId) {
    this.groupId = nullifyStringIfBlank(groupId);
  }

  @Override
  public String getGroupDescription() {
    return groupDescription;
  }

  @Override
  public void setGroupDescription(String groupDescription) {
    this.groupDescription = nullifyStringIfBlank(groupDescription);
  }

  @Override
  public Boolean isSynthetic() {
    return isSynthetic;
  }

  @Override
  public void setSynthetic(Boolean isSynthetic) {
    if (isSynthetic != null) {
      this.isSynthetic = isSynthetic;
    }
  }

  @Override
  public boolean hasNonStandardAlias() {
    return nonStandardAlias;
  }

  @Override
  public void setNonStandardAlias(boolean nonStandardAlias) {
    this.nonStandardAlias = nonStandardAlias;
  }

  @Override
  public String getDetailedQcStatusNote() {
    return detailedQcStatusNote;
  }

  @Override
  public void setDetailedQcStatusNote(String detailedQcStatusNote) {
    this.detailedQcStatusNote = detailedQcStatusNote;
  }
  
  @Override
  public Long getPreMigrationId() {
    return preMigrationId;
  }

  @Override
  public void setPreMigrationId(Long preMigrationId) {
    this.preMigrationId = preMigrationId;
  }

  @Override
  public Long getIdentityId() {
    return identityId;
  }

  @Override
  public void setIdentityId(Long identityId) {
    this.identityId = identityId;
  }

  @Override
  public Date getCreationDate() {
    return creationDate;
  }

  @Override
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  @Override
  public BigDecimal getVolumeUsed() {
    return volumeUsed;
  }

  @Override
  public void setVolumeUsed(BigDecimal ngUsed) {
    this.volumeUsed = ngUsed;
  }

  @Override
  public BigDecimal getNgUsed() {
    return ngUsed;
  }

  @Override
  public void setNgUsed(BigDecimal ngUsed) {
    this.ngUsed = ngUsed;
  }

  @Override
  public String getBarcodeSizeInfo() {
    return LimsUtils.makeVolumeAndConcentrationLabel(getVolume(), getConcentration(), getVolumeUnits(),
        getConcentrationUnits());
  }

  @Override
  public Date getBarcodeDate() {
    return Stream.of(getReceivedDate(), getCreationDate(), getCreationTime()).filter(Objects::nonNull).findFirst().orElse(null);
  }

  private Date getReceivedDate() {
    TransferItem<?> receipt = getReceiptTransfer();
    return receipt == null ? null : receipt.getTransfer().getTransferTime();
  }

  @Override
  public GroupIdentifiable getGroupIdentifiableParent() {
    return getParent();
  }

  @Override
  public String getBarcodeGroupDescription() {
    GroupIdentifiable groupParent = getEffectiveGroupIdEntity();
    return groupParent == null ? null : groupParent.getGroupDescription();
  }

  @Override
  public SampleHierarchyView getHierarchyAttributes() {
    return hierarchyAttributes;
  }

  public void setHierarchyAttributes(SampleHierarchyView hierarchyAttributes) {
    this.hierarchyAttributes = hierarchyAttributes;
  }

}
