ListTarget.tissuepiecetype = {
  name: "Tissue Piece Types",
  createUrl: function(config, projectId) {
    throw new Error("Must be provided statically");
  },
  getQueryUrl: null,
  createBulkActions: function(config, projectId) {
    var actions = HotTarget.tissuepiecetype.getBulkActions(config);
    if (config.isAdmin) {
      actions.push(ListUtils.createBulkDeleteAction('Tissue Piece Types', 'tissuepiecetypes', function(item) {
        return item.name;
      }));
    }
    return actions;
  },
  createStaticActions: function(config, projectId) {
    return config.isAdmin ? [ListUtils.createStaticAddAction('Tissue Piece Types', 'tissuepiecetype')] : [];
  },
  createColumns: function(config, projectId) {
    return [{
      sTitle: 'Name',
      mData: 'name',
      include: true,
      iSortPriority: 2
    }, {
      sTitle: 'Abbreviation',
      mData: 'abbreviation',
      include: true,
      iSortPriority: 0
    }, {
      sTitle: 'Archived',
      mData: 'archived',
      include: true,
      iSortPriority: 0,
      mRender: ListUtils.render.booleanChecks
    }];
  }
};
