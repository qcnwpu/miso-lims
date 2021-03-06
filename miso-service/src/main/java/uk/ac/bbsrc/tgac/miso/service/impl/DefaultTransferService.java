package uk.ac.bbsrc.tgac.miso.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eaglegenomics.simlims.core.Group;
import com.eaglegenomics.simlims.core.User;

import uk.ac.bbsrc.tgac.miso.core.data.AbstractBoxPosition;
import uk.ac.bbsrc.tgac.miso.core.data.Box;
import uk.ac.bbsrc.tgac.miso.core.data.Boxable;
import uk.ac.bbsrc.tgac.miso.core.data.Lab;
import uk.ac.bbsrc.tgac.miso.core.data.Library;
import uk.ac.bbsrc.tgac.miso.core.data.Pool;
import uk.ac.bbsrc.tgac.miso.core.data.Project;
import uk.ac.bbsrc.tgac.miso.core.data.Sample;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryAliquot;
import uk.ac.bbsrc.tgac.miso.core.data.impl.boxposition.LibraryAliquotBoxPosition;
import uk.ac.bbsrc.tgac.miso.core.data.impl.boxposition.LibraryBoxPosition;
import uk.ac.bbsrc.tgac.miso.core.data.impl.boxposition.PoolBoxPosition;
import uk.ac.bbsrc.tgac.miso.core.data.impl.boxposition.SampleBoxPosition;
import uk.ac.bbsrc.tgac.miso.core.data.impl.transfer.Transfer;
import uk.ac.bbsrc.tgac.miso.core.data.impl.transfer.TransferItem;
import uk.ac.bbsrc.tgac.miso.core.data.impl.transfer.TransferLibrary;
import uk.ac.bbsrc.tgac.miso.core.data.impl.transfer.TransferLibraryAliquot;
import uk.ac.bbsrc.tgac.miso.core.data.impl.transfer.TransferPool;
import uk.ac.bbsrc.tgac.miso.core.data.impl.transfer.TransferSample;
import uk.ac.bbsrc.tgac.miso.core.security.AuthorizationManager;
import uk.ac.bbsrc.tgac.miso.core.service.GroupService;
import uk.ac.bbsrc.tgac.miso.core.service.LabService;
import uk.ac.bbsrc.tgac.miso.core.service.LibraryAliquotService;
import uk.ac.bbsrc.tgac.miso.core.service.LibraryService;
import uk.ac.bbsrc.tgac.miso.core.service.PoolService;
import uk.ac.bbsrc.tgac.miso.core.service.ProviderService;
import uk.ac.bbsrc.tgac.miso.core.service.SampleService;
import uk.ac.bbsrc.tgac.miso.core.service.TransferService;
import uk.ac.bbsrc.tgac.miso.core.service.exception.ValidationError;
import uk.ac.bbsrc.tgac.miso.core.service.exception.ValidationException;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;
import uk.ac.bbsrc.tgac.miso.persistence.SaveDao;
import uk.ac.bbsrc.tgac.miso.persistence.TransferStore;
import uk.ac.bbsrc.tgac.miso.service.AbstractSaveService;

@Service
@Transactional(rollbackFor = Exception.class)
public class DefaultTransferService extends AbstractSaveService<Transfer> implements TransferService {

  private static final String ERROR_QC_NOTE_REQUIRED = "A QC note is required when QC is failed";
  private static final String ERROR_UNAUTHORIZED_ITEM_MODIFY = "Only administrators and members of the sender group can modify items";
  private static final String ERROR_UNAUTHORIZED_QC = "Only administrators and members of the recipient group can set QC results";
  private static final String ERROR_UNAUTHORIZED_RECEIPT = "Only administrators and members of the recipient group can mark receipt";
  private static final String ERROR_DISTRIBUTION_NOT_RECEIVED = "Items transferred externally must be marked received";
  private static final String ERROR_MULTIPLE_RECEIPT = "Items can only have one receipt (external lab to internal group) transfer";
  private static final String ERROR_MULTIPLE_DISTRIBUTION = "Items can only have one distribution (internal group to external named recipient) transfer";

  @Autowired
  private TransferStore transferStore;
  @Autowired
  private GroupService groupService;
  @Autowired
  private LabService labService;
  @Autowired
  private SampleService sampleService;
  @Autowired
  private LibraryService libraryService;
  @Autowired
  private LibraryAliquotService libraryAliquotService;
  @Autowired
  private PoolService poolService;
  @Autowired
  private AuthorizationManager authorizationManager;

  @Override
  public List<Transfer> list() throws IOException {
    return transferStore.list();
  }

  @Override
  public SaveDao<Transfer> getDao() {
    return transferStore;
  }

  @Override
  protected void authorizeSave(Transfer transfer) throws IOException {
    Set<Group> allowedGroups = new HashSet<>();
    if (transfer.getSenderGroup() != null) {
      allowedGroups.add(transfer.getSenderGroup());
    }
    if (transfer.getRecipientGroup() != null) {
      allowedGroups.add(transfer.getRecipientGroup());
    }
    if (allowedGroups.isEmpty()) {
      authorizationManager.throwIfNonAdmin();
    } else {
      authorizationManager.throwIfNonAdminOrGroupMember(allowedGroups);
    }
  }

  @Override
  protected void loadChildEntities(Transfer object) throws IOException {
    loadChildEntity(object.getSenderGroup(), object::setSenderGroup, groupService);
    loadChildEntity(object.getSenderLab(), object::setSenderLab, labService);
    loadChildEntity(object.getRecipientGroup(), object::setRecipientGroup, groupService);
    for (TransferSample item : object.getSampleTransfers()) {
      loadItem(item, item::setItem, sampleService, SampleBoxPosition::new, Sample::setBoxPosition);
    }
    for (TransferLibrary item : object.getLibraryTransfers()) {
      loadItem(item, item::setItem, libraryService, LibraryBoxPosition::new, Library::setBoxPosition);
    }
    for (TransferLibraryAliquot item : object.getLibraryAliquotTransfers()) {
      loadItem(item, item::setItem, libraryAliquotService, LibraryAliquotBoxPosition::new, LibraryAliquot::setBoxPosition);
    }
    for (TransferPool item : object.getPoolTransfers()) {
      loadItem(item, item::setItem, poolService, PoolBoxPosition::new, Pool::setBoxPosition);
    }
  }

  private <T extends Boxable, U extends TransferItem<T>, V extends AbstractBoxPosition> void loadItem(U item, Consumer<T> setter,
      ProviderService<T> service, Supplier<V> positionConstructor, BiConsumer<T, V> positionSetter) throws IOException {
    Box box = item.getItem().getBox();
    String position = item.getItem().getBoxPosition();
    loadChildEntity(item.getItem(), setter, service);
    transferStore.detachEntity(item.getItem());
    V boxPosition = positionConstructor.get();
    boxPosition.setBox(box);
    boxPosition.setPosition(position);
    positionSetter.accept(item.getItem(), boxPosition);
  }

  @Override
  protected void collectValidationErrors(Transfer transfer, Transfer beforeChange, List<ValidationError> errors) throws IOException {
    if (transfer.getSenderLab() == null && transfer.getSenderGroup() == null) {
      errors.add(new ValidationError("A sender lab (external) or group (internal) must be set"));
    } else if (transfer.getSenderLab() != null && transfer.getSenderGroup() != null) {
      errors.add(new ValidationError("Cannot set both sender lab (external) and group (internal)"));
    }
    if (transfer.getSenderLab() != null && transfer.getRecipient() != null) {
      errors.add(new ValidationError("A transfer cannot be external (lab) to external (named recipient)"));
    }
    if (transfer.getSenderGroup() != null && transfer.getRecipientGroup() != null
        && transfer.getSenderGroup().getId() == transfer.getRecipientGroup().getId()) {
      errors.add(new ValidationError("Sender and recipient cannot be the same group"));
    }
    if (beforeChange != null) {
      if ((beforeChange.getSenderGroup() == null) != (transfer.getSenderGroup() == null)
          && (beforeChange.getSenderLab() == null) != (transfer.getSenderLab() == null)) {
        errors.add(new ValidationError("Cannot change sender between internal (group) and external (lab)"));
      }
      if ((beforeChange.getRecipientGroup() == null) != (transfer.getRecipientGroup() == null)
          && (beforeChange.getRecipient() == null) != (transfer.getRecipient() == null)) {
        errors.add(new ValidationError("Cannot change recipient between internal (group) and external (named)"));
      }
    }
    if (!authorizationManager.isAdminUser() && transfer.getSenderGroup() != null) {
      if (!authorizationManager.isGroupMember(transfer.getSenderGroup())
          && (beforeChange == null
              || (beforeChange.getSenderGroup() != null && beforeChange.getSenderGroup().getId() != transfer.getSenderGroup().getId()))) {
        errors.add(new ValidationError("senderGroupId", "Sender group must be a group that you are a member of"));
      }
      if (beforeChange != null && beforeChange.getSenderGroup() != null
          && beforeChange.getSenderGroup().getId() != transfer.getSenderGroup().getId()
          && !authorizationManager.isGroupMember(beforeChange.getSenderGroup())) {
        errors.add(new ValidationError("senderGroupId", "Only administrators and members of the sender group can change sender group"));
      }
    }
    if (transfer.getRecipient() == null && transfer.getRecipientGroup() == null) {
      errors.add(new ValidationError("A recipient group (internal) or name (external) must be set"));
    } else if (transfer.getRecipient() != null && transfer.getRecipientGroup() != null) {
      errors.add(new ValidationError("Cannot set both recipient group (internal) and name (external)"));
    }
    if (!authorizationManager.isAdminUser() && !authorizationManager.isGroupMember(transfer.getSenderGroup())) {
      if (transfer.getRecipient() != null && !transfer.getRecipient().equals(beforeChange.getRecipient())) {
        errors.add(new ValidationError("recipient", "Only administrators and members of the sender group can change recipient"));
      }
      if (transfer.getRecipientGroup() != null && beforeChange != null && beforeChange.getRecipientGroup() != null
          && transfer.getRecipientGroup().getId() != beforeChange.getRecipientGroup().getId()) {
        errors
            .add(new ValidationError("recipientGroupId", "Only administrators and members of the sender group can change recipient group"));
      }
    }
    if (!authorizationManager.isAdminUser() && !authorizationManager.isGroupMember(transfer.getRecipientGroup())
        && transfer.getSenderLab() != null && (beforeChange == null
            || (beforeChange.getSenderLab() != null && beforeChange.getSenderLab().getId() != transfer.getSenderLab().getId()))) {
      errors.add(new ValidationError("senderLabId", "Only administrators and members of the recipient group can change sender lab"));
    }

    if (transfer.getSampleTransfers().isEmpty() && transfer.getLibraryTransfers().isEmpty()
        && transfer.getLibraryAliquotTransfers().isEmpty() && transfer.getPoolTransfers().isEmpty()) {
      errors.add(new ValidationError("items", "Transfer must include at least one item"));
    }

    if (transfer.getSenderLab() != null) {
      if (!transfer.getLibraryAliquotTransfers().isEmpty()) {
        errors.add(new ValidationError("items", "Library aliquots cannot be received directly"));
      }
      if (!transfer.getPoolTransfers().isEmpty()) {
        errors.add(new ValidationError("items", "Pools cannot be received directly"));
      }
    }

    validateItems(transfer, beforeChange, Transfer::getSampleTransfers, errors);
    validateItems(transfer, beforeChange, Transfer::getLibraryTransfers, errors);
    validateItems(transfer, beforeChange, Transfer::getLibraryAliquotTransfers, errors);
    validateItems(transfer, beforeChange, Transfer::getPoolTransfers, errors);
  }

  private <T extends Boxable, U extends TransferItem<T>> void validateItems(Transfer transfer, Transfer beforeChange,
      Function<Transfer, Set<U>> getItems, List<ValidationError> errors) throws IOException {
    Set<U> items = getItems.apply(transfer);
    Set<U> beforeChangeItems = beforeChange == null ? null : getItems.apply(beforeChange);
    if (items.stream().anyMatch(item -> Boolean.FALSE.equals(item.isQcPassed()) && LimsUtils.isStringEmptyOrNull(item.getQcNote()))) {
      errors.add(new ValidationError("items", ERROR_QC_NOTE_REQUIRED));
    }
    if (beforeChange != null && !authorizationManager.isAdminUser() && transfer.getSenderGroup() != null
        && !authorizationManager.isGroupMember(transfer.getSenderGroup())
        && (items.size() != beforeChangeItems.size() || items.stream().anyMatch(item -> beforeChangeItems.stream()
                .noneMatch(beforeItem -> beforeItem.getItem().getId() == item.getItem().getId())))) {
      errors.add(new ValidationError("items", ERROR_UNAUTHORIZED_ITEM_MODIFY));
    }
    if (!authorizationManager.isAdminUser()) {
      if (beforeChange == null) {
        if (transfer.getRecipientGroup() != null && !authorizationManager.isGroupMember(transfer.getRecipientGroup())) {
          if (items.stream().anyMatch(item -> item.isQcPassed() != null || item.getQcNote() != null)) {
            errors.add(new ValidationError("items", ERROR_UNAUTHORIZED_QC));
          }
          if (items.stream().anyMatch(item -> item.isReceived() != null)) {
            errors.add(new ValidationError("items", ERROR_UNAUTHORIZED_RECEIPT));
          }
        }
      } else {
        if (!authorizationManager.isGroupMember(transfer.getRecipientGroup()) && items.stream().anyMatch(item -> {
          U beforeChangeItem = beforeChangeItems.stream().filter(before -> before.getItem().getId() == item.getItem().getId())
              .findFirst().orElse(null);
          return beforeChangeItem != null && isChanged(item, beforeChangeItem);
        })) {
          errors.add(new ValidationError("items",
              "Only administrators and members of the recipient group can mark receipt and QC results"));
        }
      }
    }
    if (transfer.isDistribution() && items.stream().anyMatch(item -> !Boolean.TRUE.equals(item.isReceived()))) {
      errors.add(new ValidationError("items", ERROR_DISTRIBUTION_NOT_RECEIVED));
    }

    if (transfer.getSenderLab() != null) {
      if (items.stream()
          .map(TransferItem::getItem)
          .anyMatch(item -> item.getTransfers().stream()
              .map(TransferItem::getTransfer)
              .anyMatch(itemTransfer -> itemTransfer.isReceipt() && itemTransfer.isSaved()
                  && (!transfer.isSaved() || itemTransfer.getId() != transfer.getId())))) {
        errors.add(new ValidationError("items", ERROR_MULTIPLE_RECEIPT));
      }
    } else if (transfer.getRecipient() != null && items.stream()
        .map(TransferItem::getItem)
        .anyMatch(item -> item.getTransfers().stream()
            .map(TransferItem::getTransfer)
            .anyMatch(itemTransfer -> itemTransfer.isDistribution() && itemTransfer.isSaved()
                && (!transfer.isSaved() || itemTransfer.getId() != transfer.getId())))) {
                  errors.add(new ValidationError("items", ERROR_MULTIPLE_DISTRIBUTION));
    }
  }

  private <T extends Boxable, U extends TransferItem<T>> boolean isChanged(U item, U beforeChange) {
    return !Objects.equals(item.isReceived(), beforeChange.isReceived())
        || !Objects.equals(item.isQcPassed(), beforeChange.isQcPassed())
        || !Objects.equals(item.getQcNote(), beforeChange.getQcNote());
  }

  private <T extends Boxable> void validateAddition(Transfer transfer, TransferItem<T> item) throws IOException {
    List<ValidationError> errors = new ArrayList<>();

    if (Boolean.FALSE.equals(item.isQcPassed()) && LimsUtils.isStringEmptyOrNull(item.getQcNote())) {
      errors.add(new ValidationError("receiptQcNote", ERROR_QC_NOTE_REQUIRED));
    }
    if (!authorizationManager.isAdminUser() && transfer.getSenderGroup() != null
        && !authorizationManager.isGroupMember(transfer.getSenderGroup())) {
      errors.add(new ValidationError(ERROR_UNAUTHORIZED_ITEM_MODIFY));
    }
    if (transfer.getRecipientGroup() != null && !authorizationManager.isGroupMember(transfer.getRecipientGroup())) {
      if (item.isQcPassed() != null || item.getQcNote() != null) {
        errors.add(new ValidationError("receiptQcPassed", ERROR_UNAUTHORIZED_QC));
      }
      if (item.isReceived() != null) {
        errors.add(new ValidationError("received", ERROR_UNAUTHORIZED_RECEIPT));
      }
    }
    if (transfer.isDistribution() && !Boolean.TRUE.equals(item.isReceived())) {
      errors.add(new ValidationError("received", ERROR_DISTRIBUTION_NOT_RECEIVED));
    }

    if (item.getItem().getTransfers().stream()
        .map(TransferItem::getTransfer)
        .anyMatch(itemTransfer -> itemTransfer.isReceipt() && itemTransfer.getId() != transfer.getId())) {
      errors.add(new ValidationError(ERROR_MULTIPLE_RECEIPT));
    }
    if (item.getItem().getTransfers().stream()
        .map(TransferItem::getTransfer)
        .anyMatch(itemTransfer -> itemTransfer.isDistribution() && itemTransfer.getId() != transfer.getId())) {
      errors.add(new ValidationError(ERROR_MULTIPLE_DISTRIBUTION));
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  @Override
  protected void applyChanges(Transfer to, Transfer from) throws IOException {
    to.setTransferTime(from.getTransferTime());
    to.setSenderGroup(from.getSenderGroup());
    to.setSenderLab(from.getSenderLab());
    to.setRecipient(from.getRecipient());
    to.setRecipientGroup(from.getRecipientGroup());

    applyItemChanges(to, from, Transfer::getSampleTransfers);
    applyItemChanges(to, from, Transfer::getLibraryTransfers);
    applyItemChanges(to, from, Transfer::getLibraryAliquotTransfers);
    applyItemChanges(to, from, Transfer::getPoolTransfers);

    if (to.getRecipient() != null) {
      to.getSampleTransfers().forEach(item -> item.getItem().setBoxPosition(null));
      to.getLibraryTransfers().forEach(item -> item.getItem().setBoxPosition(null));
      to.getLibraryAliquotTransfers().forEach(item -> item.getItem().setBoxPosition(null));
      to.getPoolTransfers().forEach(item -> item.getItem().setBoxPosition(null));
    }
  }

  private <T extends Boxable, U extends TransferItem<T>> void applyItemChanges(Transfer to, Transfer from,
      Function<Transfer, Set<U>> getItems) throws IOException {
    Set<U> toItems = getItems.apply(to);
    Set<U> fromItems = getItems.apply(from);
    for (Iterator<U> iterator = toItems.iterator(); iterator.hasNext();) {
      U toItem = iterator.next();
      U fromItem = fromItems.stream()
          .filter(item -> item.getItem().getId() == toItem.getItem().getId())
          .findFirst().orElse(null);
      if (fromItem == null) {
        iterator.remove();
        transferStore.deleteTransferItem(toItem);
      } else {
        toItem.setReceived(fromItem.isReceived());
        toItem.setQcPassed(fromItem.isQcPassed());
        toItem.setQcNote(fromItem.getQcNote());
      }
    }

    for (U fromItem : fromItems) {
      if (toItems.stream().noneMatch(toItem -> toItem.getItem().getId() == fromItem.getItem().getId())) {
        fromItem.setTransfer(to);
        toItems.add(fromItem);
      }
    }
  }

  @Override
  protected void beforeValidate(Transfer transfer) throws IOException {
    Consumer<TransferItem<?>> action = null;
    if (transfer.isDistribution()) {
      // Mark all items received for distribution transfers
      action = item -> {
        item.setTransfer(transfer);
        item.setReceived(true);
      };
    } else {
      action = item -> item.setTransfer(transfer);
    }
    transfer.getSampleTransfers().forEach(action);
    transfer.getLibraryTransfers().forEach(action);
    transfer.getLibraryAliquotTransfers().forEach(action);
    transfer.getPoolTransfers().forEach(action);

    transfer.setChangeDetails(authorizationManager.getCurrentUser());
  }

  @Override
  protected void afterSave(Transfer object) throws IOException {
    if (object.isDistribution()) {
      // Individual services are responsible for setting volume to 0 and removing items from boxes
      for (TransferSample item : object.getSampleTransfers()) {
        if (!item.getItem().getTransfers().contains(item)) {
          item.getItem().getTransfers().add(item);
        }
        sampleService.update(item.getItem());
      }
      for (TransferLibrary item : object.getLibraryTransfers()) {
        if (!item.getItem().getTransfers().contains(item)) {
          item.getItem().getTransfers().add(item);
        }
        libraryService.update(item.getItem());
      }
      for (TransferLibraryAliquot item : object.getLibraryAliquotTransfers()) {
        if (!item.getItem().getTransfers().contains(item)) {
          item.getItem().getTransfers().add(item);
        }
        libraryAliquotService.update(item.getItem());
      }
      for (TransferPool item : object.getPoolTransfers()) {
        if (!item.getItem().getTransfers().contains(item)) {
          item.getItem().getTransfers().add(item);
        }
        poolService.update(item.getItem());
      }
    }
  }

  @Override
  public long countPendingForUser(User user) throws IOException {
    return transferStore.countPendingForGroups(user.getGroups());
  }

  @Override
  public List<Transfer> listByProperties(Lab sender, Group recipient, Project project, Date transferTime) throws IOException {
    return transferStore.listByProperties(sender, recipient, project, transferTime);
  }

  @Override
  public void addTransferSample(TransferSample transferSample) throws IOException {
    Transfer managedTransfer = get(transferSample.getTransfer().getId());
    Sample managedSample = sampleService.get(transferSample.getItem().getId());
    transferSample.setTransfer(managedTransfer);
    transferSample.setItem(managedSample);
    managedSample.getTransfers().add(transferSample);
    validateAddition(managedTransfer, transferSample);
    sampleService.update(managedSample);
    transferStore.update(managedTransfer);
  }

}
