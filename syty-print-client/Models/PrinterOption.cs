using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace SytyPrintClient.Models;

/// <summary>
/// 打印机选项（用于 UI 勾选绑定）
/// </summary>
public class PrinterOption : INotifyPropertyChanged
{
    private string _name = "";
    private bool _isSelected;

    public string Name
    {
        get => _name;
        set { _name = value; OnPropertyChanged(); }
    }

    public bool IsSelected
    {
        get => _isSelected;
        set { _isSelected = value; OnPropertyChanged(); }
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    protected void OnPropertyChanged([CallerMemberName] string? name = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));

    public override string ToString() => Name;
}
