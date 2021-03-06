-- StartNoTest
DELIMITER //

DROP TRIGGER IF EXISTS RunChange//
CREATE TRIGGER RunChange BEFORE UPDATE ON Run
FOR EACH ROW
  BEGIN
  DECLARE log_message varchar(500) CHARACTER SET utf8;
  SET log_message = CONCAT_WS(', ',
        CASE WHEN (NEW.accession IS NULL) <> (OLD.accession IS NULL) OR NEW.accession <> OLD.accession THEN CONCAT('accession: ', COALESCE(OLD.accession, 'n/a'), ' → ', COALESCE(NEW.accession, 'n/a')) END,
        CASE WHEN (NEW.alias IS NULL) <> (OLD.alias IS NULL) OR NEW.alias <> OLD.alias AND (OLD.alias NOT LIKE 'TEMPORARY%') THEN CONCAT('alias: ', COALESCE(OLD.alias, 'n/a'), ' → ', COALESCE(NEW.alias, 'n/a')) END,
        CASE WHEN (NEW.completionDate IS NULL) <> (OLD.completionDate IS NULL) OR NEW.completionDate <> OLD.completionDate THEN CONCAT('completion: ', COALESCE(OLD.completionDate, 'n/a'), ' → ', COALESCE(NEW.completionDate, 'n/a')) END,
        CASE WHEN (NEW.description IS NULL) <> (OLD.description IS NULL) OR NEW.description <> OLD.description THEN CONCAT('description: ', OLD.description, ' → ', NEW.description) END,
        CASE WHEN (NEW.filePath IS NULL) <> (OLD.filePath IS NULL) OR NEW.filePath <> OLD.filePath THEN CONCAT('file path: ', COALESCE(OLD.filePath, 'n/a'), ' → ', COALESCE(NEW.filePath, 'n/a')) END,
        CASE WHEN (NEW.health IS NULL) <> (OLD.health IS NULL) OR NEW.health <> OLD.health THEN CONCAT('health: ', COALESCE(OLD.health, 'n/a'), ' → ', COALESCE(NEW.health, 'n/a')) END,
        CASE WHEN (NEW.startDate IS NULL) <> (OLD.startDate IS NULL) OR NEW.startDate <> OLD.startDate THEN CONCAT('startDate: ', COALESCE(OLD.startDate, 'n/a'), ' → ', COALESCE(NEW.startDate, 'n/a')) END,
        CASE WHEN (NEW.sequencingParameters_parametersId IS NULL) <> (OLD.sequencingParameters_parametersId IS NULL) OR NEW.sequencingParameters_parametersId <> OLD.sequencingParameters_parametersId THEN CONCAT('parameters: ', COALESCE((SELECT name FROM SequencingParameters WHERE parametersId = OLD.sequencingParameters_parametersId), 'n/a'), ' → ', COALESCE((SELECT name FROM SequencingParameters WHERE parametersId = NEW.sequencingParameters_parametersId), 'n/a')) END,
        CASE WHEN (NEW.instrumentId IS NULL) <> (OLD.instrumentId IS NULL) OR NEW.instrumentId <> OLD.instrumentId THEN CONCAT('sequencer: ', COALESCE((SELECT name FROM Instrument WHERE instrumentId = OLD.instrumentId), 'n/a'), ' → ', COALESCE((SELECT name FROM Instrument WHERE instrumentId = NEW.instrumentId), 'n/a')) END,
        CASE WHEN (NEW.dataApproved IS NULL) <> (OLD.dataApproved IS NULL) OR NEW.dataApproved <> OLD.dataApproved THEN CONCAT('data approved: ', booleanToString(OLD.dataApproved), ' → ', booleanToString(NEW.dataApproved)) END,
        CASE WHEN (NEW.dataApproverId IS NULL) <> (OLD.dataApproverId IS NULL) OR NEW.dataApproverId <> OLD.dataApproverId THEN CONCAT('data approver: ', COALESCE((SELECT fullName FROM User WHERE userId = OLD.dataApproverId), 'n/a'), ' → ', COALESCE((SELECT fullName FROM User WHERE userId = NEW.dataApproverId), 'n/a')) END);
  IF log_message IS NOT NULL AND log_message <> '' THEN
    INSERT INTO RunChangeLog(runId, columnsChanged, userId, message, changeTime) VALUES (
      NEW.runId,
      COALESCE(CONCAT_WS(',',
        CASE WHEN (NEW.accession IS NULL) <> (OLD.accession IS NULL) OR NEW.accession <> OLD.accession THEN 'accession' END,
        CASE WHEN (NEW.alias IS NULL) <> (OLD.alias IS NULL) OR NEW.alias <> OLD.alias THEN 'alias' END,
        CASE WHEN (NEW.completionDate IS NULL) <> (OLD.completionDate IS NULL) OR NEW.completionDate <> OLD.completionDate THEN 'completionDate' END,
        CASE WHEN (NEW.description IS NULL) <> (OLD.description IS NULL) OR NEW.description <> OLD.description THEN 'description' END,
        CASE WHEN (NEW.filePath IS NULL) <> (OLD.filePath IS NULL) OR NEW.filePath <> OLD.filePath THEN 'filePath' END,
        CASE WHEN (NEW.health IS NULL) <> (OLD.health IS NULL) OR NEW.health <> OLD.health THEN 'health' END,
        CASE WHEN (NEW.metrics IS NULL) <> (OLD.metrics IS NULL) OR NEW.metrics <> OLD.metrics THEN 'metrics' END,
        CASE WHEN (NEW.startDate IS NULL) <> (OLD.startDate IS NULL) OR NEW.startDate <> OLD.startDate THEN 'startDate' END,
        CASE WHEN (NEW.sequencingParameters_parametersId IS NULL) <> (OLD.sequencingParameters_parametersId IS NULL) OR NEW.sequencingParameters_parametersId <> OLD.sequencingParameters_parametersId THEN 'parameters' END,
        CASE WHEN (NEW.instrumentId IS NULL) <> (OLD.instrumentId IS NULL) OR NEW.instrumentId <> OLD.instrumentId THEN 'instrumentId' END,
        CASE WHEN (NEW.dataApproved IS NULL) <> (OLD.dataApproved IS NULL) OR NEW.dataApproved <> OLD.dataApproved THEN 'dataApproved' END,
        CASE WHEN (NEW.dataApproverId IS NULL) <> (OLD.dataApproverId IS NULL) OR NEW.dataApproverId <> OLD.dataApproverId THEN 'dataApproverId' END), ''),
      NEW.lastModifier,
      log_message,
      NEW.lastModified);
  END IF;
  END//

DROP TRIGGER IF EXISTS RunChangeLS454//
CREATE TRIGGER RunChangeLS454 BEFORE UPDATE ON RunLS454
FOR EACH ROW
  BEGIN
  DECLARE log_message varchar(500) CHARACTER SET utf8;
  SET log_message = CONCAT_WS(', ',
        CASE WHEN NEW.pairedEnd <> OLD.pairedEnd THEN CONCAT('ends: ', CASE WHEN OLD.pairedEnd THEN 'paired' ELSE 'single' END, ' → ', CASE WHEN NEW.pairedEnd THEN 'paired' ELSE 'single' END) END,
        CASE WHEN (NEW.cycles IS NULL) <> (OLD.cycles IS NULL) OR NEW.cycles <> OLD.cycles THEN CONCAT('cycles: ', COALESCE(OLD.cycles, 'n/a'), ' → ', COALESCE(NEW.cycles, 'n/a')) END);
  IF log_message IS NOT NULL AND log_message <> '' THEN
    INSERT INTO RunChangeLog(runId, columnsChanged, userId, message, changeTime)
    SELECT
      NEW.runId,
      COALESCE(CONCAT_WS(',',
        CASE WHEN NEW.pairedEnd <> OLD.pairedEnd THEN 'pairedend' END,
        CASE WHEN (NEW.cycles IS NULL) <> (OLD.cycles IS NULL) OR NEW.cycles <> OLD.cycles THEN 'cycles' END), ''),
      lastModifier,
      log_message,
      lastModified
    FROM Run WHERE Run.runId = NEW.runId;
  END IF;
  END//
  
DROP TRIGGER IF EXISTS RunChangeSolid//
CREATE TRIGGER RunChangeSolid BEFORE UPDATE ON RunSolid
FOR EACH ROW
  BEGIN
  DECLARE log_message varchar(500) CHARACTER SET utf8;
  SET log_message = CONCAT_WS(', ',
        CASE WHEN NEW.pairedEnd <> OLD.pairedEnd THEN CONCAT('ends: ', CASE WHEN OLD.pairedEnd THEN 'paired' ELSE 'single' END, ' → ', CASE WHEN NEW.pairedEnd THEN 'paired' ELSE 'single' END) END);
  IF log_message IS NOT NULL AND log_message <> '' THEN
    INSERT INTO RunChangeLog(runId, columnsChanged, userId, message, changeTime)
    SELECT
      NEW.runId,
      COALESCE(CONCAT_WS(',',
        CASE WHEN NEW.pairedEnd <> OLD.pairedEnd THEN 'pairedend' END), ''),
      lastModifier,
      log_message,
      lastModified
    FROM Run WHERE Run.runId = NEW.runId;
  END IF;
  END//

DROP TRIGGER IF EXISTS RunChangeIllumina//
CREATE TRIGGER RunChangeIllumina BEFORE UPDATE ON RunIllumina
FOR EACH ROW
  BEGIN
  DECLARE log_message varchar(500) CHARACTER SET utf8;
  -- Note: cycles are not change logged as they are expected to change a lot via Run Scanner during a run 
  SET log_message = CONCAT_WS(', ',
        CASE WHEN NEW.pairedEnd <> OLD.pairedEnd THEN CONCAT('ends: ', CASE WHEN OLD.pairedEnd THEN 'paired' ELSE 'single' END, ' → ', CASE WHEN NEW.pairedEnd THEN 'paired' ELSE 'single' END) END,
        CASE WHEN (NEW.runBasesMask IS NULL) <> (OLD.runBasesMask IS NULL) OR NEW.runBasesMask <> OLD.runBasesMask THEN CONCAT('run bases mask: ', COALESCE(OLD.runBasesMask, 'n/a'), ' → ', COALESCE(NEW.runBasesMask, 'n/a')) END,
        CASE WHEN (NEW.workflowType IS NULL) <> (OLD.workflowType IS NULL) OR NEW.workflowType <> OLD.workflowType THEN CONCAT('workflow type: ', COALESCE(OLD.workflowType, 'n/a'), ' → ', COALESCE(NEW.workflowType, 'n/a')) END);
  IF log_message IS NOT NULL AND log_message <> '' THEN
    INSERT INTO RunChangeLog(runId, columnsChanged, userId, message, changeTime)
    SELECT
      NEW.runId,
      COALESCE(CONCAT_WS(',',
        CASE WHEN NEW.pairedEnd <> OLD.pairedEnd THEN 'pairedEnd' END,
        CASE WHEN (NEW.runBasesMask IS NULL) <> (OLD.runBasesMask IS NULL) OR NEW.runBasesMask <> OLD.runBasesMask THEN 'runBasesMask' END,
        CASE WHEN (NEW.workflowType IS NULL) <> (OLD.workflowType IS NULL) OR NEW.workflowType <> OLD.workflowType THEN 'workflowType' END), ''),
      lastModifier,
      log_message,
      lastModified
    FROM Run WHERE Run.runId = NEW.runId;
  END IF;
  END//

DROP TRIGGER IF EXISTS RunChangeOxfordNanopore//
CREATE TRIGGER RunChangeOxfordNanopore BEFORE UPDATE ON RunOxfordNanopore
FOR EACH ROW
  BEGIN
  DECLARE log_message varchar(500) CHARACTER SET utf8;
  SET log_message = CONCAT_WS(', ',
        CASE WHEN (NEW.minKnowVersion IS NULL) <> (OLD.minKnowVersion IS NULL) OR NEW.minKnowVersion <> OLD.minKnowVersion THEN CONCAT('MinKNOW version: ', COALESCE(OLD.minKnowVersion, 'n/a'), ' → ', COALESCE(NEW.minKnowVersion, 'n/a')) END,
        CASE WHEN (NEW.protocolVersion IS NULL) <> (OLD.protocolVersion IS NULL) OR NEW.protocolVersion <> OLD.protocolVersion THEN CONCAT('Protocol version: ', COALESCE(OLD.protocolVersion, 'n/a'), ' → ', COALESCE(NEW.protocolVersion, 'n/a')) END);
  IF log_message IS NOT NULL AND log_message <> '' THEN
    INSERT INTO RunChangeLog(runId, columnsChanged, userId, message, changeTime)
    SELECT
      NEW.runId,
      COALESCE(CONCAT_WS(',',
        CASE WHEN (NEW.minKnowVersion IS NULL) <> (OLD.minKnowVersion IS NULL) OR NEW.minKnowVersion <> OLD.minKnowVersion THEN 'MinKNOW version' END,
        CASE WHEN (NEW.protocolVersion IS NULL) <> (OLD.protocolVersion IS NULL) OR NEW.protocolVersion <> OLD.protocolVersion THEN 'Protocol version' END), ''),
      lastModifier,
      log_message,
      lastModified
    FROM Run WHERE Run.runId = NEW.runId;
  END IF;
  END//

DROP TRIGGER IF EXISTS RunInsert//
CREATE TRIGGER RunInsert AFTER INSERT ON Run
FOR EACH ROW
  INSERT INTO RunChangeLog(runId, columnsChanged, userId, message, changeTime) VALUES (
    NEW.runId,
    '',
    NEW.lastModifier,
    'Run created.',
    NEW.lastModified)//
DELIMITER ;
-- EndNoTest
