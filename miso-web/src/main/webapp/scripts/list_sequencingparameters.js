ListTarget.sequencingparameters = {
  name: "Sequencing Parameters",
  createUrl: function(config, projectId) {
    throw new Error("Must be provided statically");
  },
  getQueryUrl: null,
  createBulkActions: function(config, projectId) {
    var actions = HotTarget.sequencingparameters.getBulkActions(config);
    if (config.isAdmin) {
      actions.push(ListUtils.createBulkDeleteAction('Sequencing Parameters', 'sequencingparameters', function(item) {
        return item.name + ' (' + item.instrumentModelAlias + ')';
      }));
    }
    return actions;
  },
  createStaticActions: function(config, projectId) {
    return config.isAdmin ? [ListUtils.createStaticAddAction('Sequencing Parameters', 'sequencingparameters')] : [];
  },
  createColumns: function(config, projectId) {
    return [{
      sTitle: 'Name',
      mData: 'name',
      include: true,
      iSortPriority: 2
    }, {
      sTitle: 'Instrument Model',
      mData: 'instrumentModelAlias',
      include: true,
      iSortPriority: 1
    }];
  }
};
