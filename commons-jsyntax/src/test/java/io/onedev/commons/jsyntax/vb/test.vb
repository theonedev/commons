VERSION 5.00
Object = "{831FDD16-0C5C-11D2-A9FC-0000F8754DA1}#2.0#0"; "MSCOMCTL.OCX"
Begin VB.Form MainFrm 
   Caption         =   "ACMÔÂÈüÊµÊ±ÅÅÃû"
   ClientHeight    =   6105
   ClientLeft      =   60
   ClientTop       =   405
   ClientWidth     =   14025
   LinkTopic       =   "Form1"
   ScaleHeight     =   6105
   ScaleWidth      =   14025
   StartUpPosition =   3  '´°¿ÚÈ±Ê¡
   Begin MSComctlLib.ListView ListView1 
      Height          =   5175
      Index           =   0
      Left            =   120
      TabIndex        =   12
      Top             =   720
      Width           =   5895
      _ExtentX        =   10398
      _ExtentY        =   9128
      View            =   3
      LabelWrap       =   -1  'True
      HideSelection   =   -1  'True
      FullRowSelect   =   -1  'True
      _Version        =   393217
      ForeColor       =   -2147483640
      BackColor       =   -2147483643
      BorderStyle     =   1
      Appearance      =   1
      BeginProperty Font {0BE35203-8F91-11CE-9DE3-00AA004BB851} 
         Name            =   "Î¢ÈíÑÅºÚ"
         Size            =   12
         Charset         =   134
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      NumItems        =   0
   End
   Begin VB.TextBox Text5 
      BeginProperty Font 
         Name            =   "Î¢ÈíÑÅºÚ"
         Size            =   12
         Charset         =   134
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   375
      Left            =   12720
      TabIndex        =   10
      Text            =   "0?15*"
      Top             =   120
      Width           =   1095
   End
   Begin VB.Timer Timer1 
      Enabled         =   0   'False
      Interval        =   10000
      Left            =   12960
      Top             =   1320
   End
   Begin VB.TextBox Text4 
      BeginProperty Font 
         Name            =   "Î¢ÈíÑÅºÚ"
         Size            =   12
         Charset         =   134
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   375
      Left            =   9480
      TabIndex        =   9
      Text            =   "0.3"
      Top             =   120
      Width           =   615
   End
   Begin VB.TextBox Text3 
      BeginProperty Font 
         Name            =   "Î¢ÈíÑÅºÚ"
         Size            =   12
         Charset         =   134
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   375
      Left            =   7680
      TabIndex        =   8
      Text            =   "0.2"
      Top             =   120
      Width           =   615
   End
   Begin VB.TextBox Text2 
      BeginProperty Font 
         Name            =   "Î¢ÈíÑÅºÚ"
         Size            =   12
         Charset         =   134
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   375
      Left            =   5880
      TabIndex        =   7
      Text            =   "0.1"
      Top             =   120
      Width           =   615
   End
   Begin VB.CheckBox Check1 
      Caption         =   "°üº¬ÌúÅÆ"
      Height          =   255
      Left            =   10320
      TabIndex        =   6
      Top             =   240
      Value           =   1  'Checked
      Width           =   1215
   End
   Begin VB.CommandButton Command1 
      Caption         =   "Go"
      Height          =   375
      Left            =   3240
      TabIndex        =   2
      Top             =   120
      Width           =   1095
   End
   Begin VB.TextBox Text1 
      BeginProperty Font 
         Name            =   "Î¢ÈíÑÅºÚ"
         Size            =   12
         Charset         =   134
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   375
      Left            =   1440
      TabIndex        =   1
      Top             =   120
      Width           =   1575
   End
   Begin MSComctlLib.ListView ListView1 
      Height          =   5175
      Index           =   1
      Left            =   6120
      TabIndex        =   13
      Top             =   720
      Width           =   5895
      _ExtentX        =   10398
      _ExtentY        =   9128
      View            =   3
      LabelWrap       =   -1  'True
      HideSelection   =   -1  'True
      FullRowSelect   =   -1  'True
      _Version        =   393217
      ForeColor       =   -2147483640
      BackColor       =   -2147483643
      BorderStyle     =   1
      Appearance      =   1
      BeginProperty Font {0BE35203-8F91-11CE-9DE3-00AA004BB851} 
         Name            =   "Î¢ÈíÑÅºÚ"
         Size            =   12
         Charset         =   134
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      NumItems        =   0
   End
   Begin VB.Label Label5 
      Caption         =   "·Ö×éÌõ¼þ£º"
      Height          =   255
      Left            =   11760
      TabIndex        =   11
      Top             =   240
      Width           =   1095
   End
   Begin VB.Label Label4 
      Caption         =   "  ÒøÅÆ±ÈÀý£º"
      Height          =   255
      Left            =   6600
      TabIndex        =   5
      Top             =   240
      Width           =   1095
   End
   Begin VB.Label Label3 
      Caption         =   "  Í­ÅÆ±ÈÀý£º"
      Height          =   255
      Left            =   8400
      TabIndex        =   4
      Top             =   240
      Width           =   1095
   End
   Begin VB.Label Label2 
      Caption         =   "  ½ðÅÆ±ÈÀý£º"
      Height          =   255
      Left            =   4800
      TabIndex        =   3
      Top             =   240
      Width           =   1095
   End
   Begin VB.Label Label1 
      Caption         =   "Contest ID:"
      Height          =   255
      Left            =   240
      TabIndex        =   0
      Top             =   240
      Width           =   1335
   End
End
Attribute VB_Name = "MainFrm"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Option Explicit

Private Type Item
    id As String
    nick As String
    penalty As String
    solved As Integer
End Type

Dim prize(3) As String
Dim items(1, 1000) As Item
Dim cnt(1) As Integer

Private Sub readRank()
    Dim url$, html$
    Dim xml As New XMLHTTP
    url = "http://acm.cumtcs.net/JudgeOnline/contestrank.php?cid=" + Text1.Text
    xml.open "GET", url, False
    xml.send
    html = xml.responseText
    Dim pos1&, pos2&
    cnt(0) = 0
    cnt(1) = 0
    pos1 = InStr(1, html, "RankList")
    If pos1 = 0 Then
        MsgBox "No Such Contest!"
        Exit Sub
    End If
    pos2 = InStr(pos1, html, "</title>")
    Me.Caption = "ACMÔÂÈüÊµÊ±ÅÅÃû" + Mid(html, pos1 + 8, pos2 - pos1 - 8)
    Do
        pos1 = InStr(pos2, html, "?user=")
        If pos1 = 0 Then Exit Do
        pos1 = InStr(pos1, html, ">")
        pos2 = InStr(pos1, html, "</a>")
        items(0, cnt(0)).id = Mid(html, pos1 + 1, pos2 - pos1 - 1)
        pos1 = InStr(pos2, html, "?user=")
        pos1 = InStr(pos1, html, ">")
        pos2 = InStr(pos1, html, "</a>")
        items(0, cnt(0)).nick = Mid(html, pos1 + 1, pos2 - pos1 - 1)
        pos1 = InStr(pos2, html, "?user_id=")
        pos1 = InStr(pos1, html, ">")
        pos2 = InStr(pos1, html, "</a>")
        items(0, cnt(0)).solved = Mid(html, pos1 + 1, pos2 - pos1 - 1)
        pos1 = InStr(pos2, html, "<td>")
        pos2 = InStr(pos1, html, "<td ")
        items(0, cnt(0)).penalty = Mid(html, pos1 + 4, pos2 - pos1 - 4)
        'If items(0, cnt(0)).solved = 0 Then Exit Do
        If items(0, cnt(0)).id Like Text5.Text Or items(0, cnt(0)).id Like "l*" Or items(0, cnt(0)).id Like "J*" Then
            items(1, cnt(1)) = items(0, cnt(0))
            cnt(1) = cnt(1) + 1
        Else
            cnt(0) = cnt(0) + 1
        End If
    Loop
End Sub

Private Sub updateList()
    Dim Item As ListItem
    Dim info As Object
    Dim i&, c&
    Dim subitem As Object
    readRank
    For c = 0 To 1
        ListView1(c).ListItems.Clear
        For i = 0 To cnt(c) - 1
            Set Item = ListView1(c).ListItems.Add()
            Item.SubItems(1) = i + 1
            Item.SubItems(2) = items(c, i).id
            Item.SubItems(3) = items(c, i).nick
            Item.SubItems(4) = items(c, i).penalty
            Item.SubItems(5) = items(c, i).solved
            If i < cnt(c) * CDbl(Text2.Text) Then
                Item.Text = prize(0)
                Item.ForeColor = RGB(240, 202, 0)
                For Each subitem In Item.ListSubItems
                    subitem.ForeColor = Item.ForeColor
                Next
            ElseIf i < cnt(c) * (CDbl(Text3.Text) + CDbl(Text2.Text)) Then
                Item.Text = prize(1)
                Item.ForeColor = RGB(150, 160, 165)
                For Each subitem In Item.ListSubItems
                    subitem.ForeColor = Item.ForeColor
                Next
            ElseIf i < cnt(c) * (CDbl(Text4.Text) + CDbl(Text3.Text) + CDbl(Text2.Text)) Then
                Item.Text = prize(2)
                Item.ForeColor = RGB(201, 104, 0)
                For Each subitem In Item.ListSubItems
                    subitem.ForeColor = Item.ForeColor
                Next
            ElseIf Check1.Value = Checked Then
                Item.Text = prize(3)
            End If
        Next
    Next
End Sub

Private Sub Form_Load()
    prize(0) = "½ðÅÆ"
    prize(1) = "ÒøÅÆ"
    prize(2) = "Í­ÅÆ"
    prize(3) = "ÌúÅÆ"
    Dim i&
    For i = 0 To 1
        ListView1(i).ColumnHeaders.Add , , "½±Ïî", 15 * 70
        ListView1(i).ColumnHeaders.Add , , "ÅÅÃû", 15 * 60
        ListView1(i).ColumnHeaders.Add , , "ID", 15 * 150
        ListView1(i).ColumnHeaders.Add , , "êÇ³Æ", 15 * 250
        ListView1(i).ColumnHeaders.Add , , "·£Ê±", 15 * 110
        ListView1(i).ColumnHeaders.Add , , "½âÌâÊýÁ¿", 15 * 80
    Next
End Sub

Private Sub Command1_Click()
    If Command1.Caption = "Go" Then
        Command1.Caption = "Stop"
        updateList
        Timer1.Enabled = True
    Else
        Command1.Caption = "Go"
        Timer1.Enabled = False
    End If
End Sub

Private Sub Form_Resize()
    ListView1(0).Width = (Me.Width - 360) / 2
    ListView1(1).Width = (Me.Width - 360) / 2
    ListView1(1).Left = (Me.Width - 360) / 2
    ListView1(0).Height = Me.Height - 1300
    ListView1(1).Height = Me.Height - 1300
End Sub

Private Sub Timer1_Timer()
    updateList
End Sub
