#include "ChildName.h"

void ChildName ::SetName(DOMString name)
{
	Name = name;
};
void ChildName::SetNext(ChildName* next)
{
	Next = next;
};