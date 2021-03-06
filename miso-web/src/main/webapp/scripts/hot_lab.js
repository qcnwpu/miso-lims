HotTarget.lab = {
  getCreateUrl: function() {
    return Urls.rest.labs.create;
  },
  getUpdateUrl: function(id) {
    return Urls.rest.labs.update(id);
  },
  requestConfiguration: function(config, callback) {
    callback(config)
  },
  fixUp: function(lab, errorHandler) {
  },
  createColumns: function(config, create, data) {
    return [
        HotUtils.makeColumnForText('Name', true, 'alias', {
          unpackAfterSave: true,
          validator: HotUtils.validator.requiredText
        }),
        HotUtils.makeColumnForConstantsList('Institute', true, 'instituteAlias', 'instituteId', 'id', 'alias', config.institutes, true, {},
            null), ];
  },

  getBulkActions: function(config) {
    return !config.isAdmin ? [] : [{
      name: 'Edit',
      action: function(items) {
        window.location = window.location.origin + '/miso/lab/bulk/edit?' + jQuery.param({
          ids: items.map(Utils.array.getId).join(',')
        });
      }
    },

    {
      name: 'Delete',
      action: function(items) {
        var deleteNext = function(index) {
          if (index == items.length) {
            window.location = window.location.origin + '/miso/lab/list';
            return;
          }
          Utils.ajaxWithDialog('Deleting ' + items[index].alias, 'DELETE', '/miso/rest/labs/' + items[index].id, null, function() {
            deleteNext(index + 1);
          });
        };
        deleteNext(0);
      }
    }, ];
  }
};
