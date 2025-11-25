class PageDTO<T> {
  int pageNo;
  int pageSize;
  int total;
  List<T> list;

  PageDTO({required this.pageNo, required this.pageSize, required this.total, required this.list});

  factory PageDTO.fromJson(Map<String, dynamic> json, T Function(Object? json) fromJsonT) {
    // 自定义处理total字段，支持int和String类型
    int totalValue = 0;
    final totalJson = json['total'];
    if (totalJson != null) {
      if (totalJson is int) {
        totalValue = totalJson;
      } else if (totalJson is String) {
        totalValue = int.tryParse(totalJson) ?? 0;
      } else if (totalJson is num) {
        totalValue = totalJson.toInt();
      }
    }

    return PageDTO<T>(
      pageNo: (json['pageNo'] as num?)?.toInt() ?? 1,
      pageSize: (json['pageSize'] as num?)?.toInt() ?? 10,
      total: totalValue,
      list: (json['list'] as List<dynamic>?)?.map(fromJsonT).toList() ?? [],
    );
  }

  Map<String, dynamic> toJson(Object? Function(T value) toJsonT) {
    return {'pageNo': pageNo, 'pageSize': pageSize, 'total': total, 'list': list.map(toJsonT).toList()};
  }
}
